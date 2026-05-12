package com.cdc.service;

import com.cdc.failure.FailureRecorder;
import com.entity.resource_entities.Resource;
import com.entity_enums.resource_enums.EmploymentStatus;
import com.entity_enums.resource_enums.EmploymentType;
import com.entity_enums.resource_enums.WorkingMode;
import com.repo.resource_repo.ResourceRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.connect.data.Struct;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

@Service
@Slf4j
public class EosResourceSyncService {

    private final ResourceRepository resourceRepository;
    private final FailureRecorder failureRecorder;
    private final JdbcTemplate eosJdbcTemplate;

    public EosResourceSyncService(
            ResourceRepository resourceRepository,
            FailureRecorder failureRecorder,
            @Qualifier("eosJdbcTemplate") JdbcTemplate eosJdbcTemplate) {
        this.resourceRepository = resourceRepository;
        this.failureRecorder = failureRecorder;
        this.eosJdbcTemplate = eosJdbcTemplate;
    }

    // -------------------------------------------------------------------------
    // Public entry points (one per EOS table)
    // -------------------------------------------------------------------------

    @Transactional
    public void processEmployeeDetails(Struct after) {

        String employeeId = getString(after, "employee_id");
        if (employeeId == null) return;

        // Use pessimistic lock when the resource already exists to prevent
        // concurrent updates in multi-instance deployments.
        Resource resource = resourceRepository.findByIdWithLock(employeeId)
                .orElseGet(Resource::new);

        boolean isNew = (resource.getVersion() == null);
        resource.setResourceId(employeeId);

        applyEmployeeDetailsMapping(resource, after);   // direct field copy
        applyDerivedFields(resource, after);            // employmentType, workingMode, employmentStatus, activeFlag

        // Guard NOT NULL columns against unrecognized EOS values
        if (isNew) {
            if (resource.getFullName() == null || resource.getFullName().isBlank())
                resource.setFullName(employeeId);
            if (resource.getEmploymentType() == null)
                resource.setEmploymentType(EmploymentType.FULL_TIME);
            if (resource.getEmploymentStatus() == null)
                resource.setEmploymentStatus(EmploymentStatus.ACTIVE);
            if (resource.getActiveFlag() == null)
                resource.setActiveFlag(true);
            LocalDateTime srcCreatedAt = getLocalDateTime(after, "created_at");
            resource.setCreatedAt(srcCreatedAt != null ? srcCreatedAt : LocalDateTime.now());
        }

        LocalDateTime srcChangedAt = getLocalDateTime(after, "updated_at");
        resource.setChangedAt(srcChangedAt != null ? srcChangedAt : LocalDateTime.now());

        resourceRepository.save(resource);
        log.info("employee_details synced successfully for resourceId={}", employeeId);
    }

    @Transactional
    public void processOfferDetails(Struct after) {

        String mail = getString(after, "mail");
        if (mail == null) return;

        // offer_letter_details has no employee_id column.
        // Link: offer_letter_details.user_uuid → employee_details.user_uuid → employee_id.
        String userUuid = getString(after, "user_uuid");
        String employeeId = resolveEmployeeIdByUserUuid(userUuid);

        Resource resource = null;
        if (employeeId != null) {
            resource = resourceRepository.findById(employeeId).orElse(null);
        }
        if (resource == null) {
            failureRecorder.recordFailure(
                    "EOS-offer_letter_details", mail, "UPDATE",
                    "ResourceNotFound",
                    "Resource not found for offer letter (mail=" + mail
                            + ", userUuid=" + userUuid + ", employeeId=" + employeeId + ")",
                    null);
            return;
        }

        resource.setDesignation(getString(after, "designation"));
        resource.setAnnualCtc(getBigDecimal(after, "total_ctc"));
        resource.setCurrencyType(getString(after, "currency"));

        Long changedBy = getLong(after, "created_by");
        if (changedBy != null) resource.setChangedBy(changedBy);

        deriveHourlyCostRate(resource);

        LocalDateTime srcChangedAt = getLocalDateTime(after, "updated_at");
        resource.setChangedAt(srcChangedAt != null ? srcChangedAt : LocalDateTime.now());

        resourceRepository.save(resource);
        log.info("offer_letter_details enriched for resourceId={}, mail={}", resource.getResourceId(), mail);
    }

    @Transactional
    public void processEmployeeExit(Struct after) {

        String employeeId = getString(after, "employee_id");
        if (employeeId == null) return;

        Resource resource = resourceRepository.findById(employeeId).orElse(null);
        if (resource == null) {
            // Out-of-order event: exit arrived before employee_details was processed.
            failureRecorder.recordFailure(
                    "EOS-employee_exit", employeeId, "UPDATE",
                    "ResourceNotFound",
                    "Resource not found for employeeId: " + employeeId + "; event may be out-of-order",
                    null);
            return;
        }

        resource.setDateOfExit(getLocalDate(after, "last_working_day"));
        resource.setNoticeStartDate(getLocalDate(after, "notice_start_date"));
        resource.setNoticeEndDate(getLocalDate(after, "notice_end_date"));

        Long changedBy = getLong(after, "created_by");
        if (changedBy != null) resource.setChangedBy(changedBy);

        deriveEmploymentStatus(resource, after);

        LocalDateTime srcChangedAt = getLocalDateTime(after, "updated_at");
        resource.setChangedAt(srcChangedAt != null ? srcChangedAt : LocalDateTime.now());

        resourceRepository.save(resource);
        log.info("employee_exit processed successfully for resourceId={}", employeeId);
    }

    @Transactional
    public void handleDelete(String tableName, Struct before) {

        if (before == null) return;
        if (!"employee_details".equals(tableName)) return;

        String employeeId = getString(before, "employee_id");
        if (employeeId == null) return;

        Resource resource = resourceRepository.findById(employeeId).orElse(null);
        if (resource == null) return;

        resource.setActiveFlag(false);
        resource.setEmploymentStatus(EmploymentStatus.EXITED);
        resource.setDateOfExit(LocalDate.now());

        Long changedBy = getLong(before, "created_by");
        if (changedBy != null) resource.setChangedBy(changedBy);

        resource.setChangedAt(LocalDateTime.now());

        resourceRepository.save(resource);
        log.info("Soft delete completed for resourceId={}", employeeId);
    }

    // -------------------------------------------------------------------------
    // Private mapping helpers
    // -------------------------------------------------------------------------

    private void applyEmployeeDetailsMapping(Resource resource, Struct after) {

        String firstName  = getString(after, "first_name");
        String middleName = getString(after, "middle_name");
        String lastName   = getString(after, "last_name");

        String fullName = buildFullName(firstName, middleName, lastName);
        if (fullName != null && !fullName.isEmpty()) {
            resource.setFullName(fullName);
        }

        resource.setEmail(getString(after, "work_email"));
        resource.setWorkingLocation(getString(after, "location"));
        resource.setExperiance(getDouble(after, "total_experience"));
        resource.setDateOfJoining(getLocalDate(after, "joining_date"));

        Long changedBy = getLong(after, "created_by");
        if (changedBy != null) {
            resource.setChangedBy(changedBy);
        }
    }

    private void applyDerivedFields(Resource resource, Struct after) {

        String employmentType   = getString(after, "employment_type");
        String workMode         = getString(after, "work_mode");
        String employmentStatus = getString(after, "employment_status");

        if ("Full-Time".equalsIgnoreCase(employmentType)) {
            resource.setEmploymentType(EmploymentType.FULL_TIME);
        } else if ("Part-Time".equalsIgnoreCase(employmentType)) {
            resource.setEmploymentType(EmploymentType.PART_TIME);
        } else if ("Intern".equalsIgnoreCase(employmentType)) {
            resource.setEmploymentType(EmploymentType.INTERN);
        } else if ("Contractor".equalsIgnoreCase(employmentType)) {
            resource.setEmploymentType(EmploymentType.CONTRACTOR);
        } else if ("Freelance".equalsIgnoreCase(employmentType)) {
            resource.setEmploymentType(EmploymentType.FREELANCE);
        }

        if ("Office".equalsIgnoreCase(workMode)) {
            resource.setWorkingMode(WorkingMode.OFFICE);
        } else if ("Remote".equalsIgnoreCase(workMode)) {
            resource.setWorkingMode(WorkingMode.REMOTE);
        } else if ("Hybrid".equalsIgnoreCase(workMode)) {
            resource.setWorkingMode(WorkingMode.HYBRID);
        }

        if ("Active".equalsIgnoreCase(employmentStatus)) {
            resource.setEmploymentStatus(EmploymentStatus.ACTIVE);
            resource.setActiveFlag(true);
        } else if ("Probation".equalsIgnoreCase(employmentStatus)) {
            resource.setEmploymentStatus(EmploymentStatus.PROBATION);
            resource.setActiveFlag(true);
        } else if ("Resigned".equalsIgnoreCase(employmentStatus)) {
            resource.setEmploymentStatus(EmploymentStatus.RESIGNED);
            resource.setActiveFlag(false);
        } else if ("Terminated".equalsIgnoreCase(employmentStatus)) {
            resource.setEmploymentStatus(EmploymentStatus.TERMINATED);
            resource.setActiveFlag(false);
        } else if ("Absconded".equalsIgnoreCase(employmentStatus)) {
            resource.setEmploymentStatus(EmploymentStatus.ABSCONDED);
            resource.setActiveFlag(false);
        }
    }

    private void deriveEmploymentStatus(Resource resource, Struct after) {

        String exitType = getString(after, "exit_type");
        String status   = getString(after, "status");

        if ("Termination".equalsIgnoreCase(exitType)) {
            resource.setEmploymentStatus(EmploymentStatus.TERMINATED);
            resource.setActiveFlag(false);
        } else if ("Absconded".equalsIgnoreCase(exitType)) {
            resource.setEmploymentStatus(EmploymentStatus.ABSCONDED);
            resource.setActiveFlag(false);
        } else if ("Resignation".equalsIgnoreCase(exitType)) {
            resource.setEmploymentStatus(EmploymentStatus.RESIGNED);
            resource.setActiveFlag(true);
        }

        if ("Completed".equalsIgnoreCase(status)) {
            resource.setEmploymentStatus(EmploymentStatus.EXITED);
            resource.setActiveFlag(false);
        } else {
            resource.setEmploymentStatus(EmploymentStatus.ON_NOTICE);
            resource.setActiveFlag(true);
        }
    }

    private void deriveHourlyCostRate(Resource resource) {

        BigDecimal annualCtc = resource.getAnnualCtc();
        if (annualCtc == null || annualCtc.compareTo(BigDecimal.ZERO) <= 0) return;

        BigDecimal hourlyRate = annualCtc
                .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(22), 2, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(8),  2, RoundingMode.HALF_UP);

        resource.setHourlyCostRate(hourlyRate);
    }

    // -------------------------------------------------------------------------
    // Struct extraction helpers
    // -------------------------------------------------------------------------

    private String getString(Struct struct, String fieldName) {
        if (struct == null || struct.schema().field(fieldName) == null) return null;
        Object value = struct.get(fieldName);
        return value != null ? value.toString() : null;
    }

    private Long getLong(Struct struct, String fieldName) {
        if (struct == null || struct.schema().field(fieldName) == null) return null;
        Object value = struct.get(fieldName);
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal getBigDecimal(Struct struct, String fieldName) {
        if (struct == null || struct.schema().field(fieldName) == null) return null;
        Object value = struct.get(fieldName);
        if (value == null) return null;
        try {
            return new BigDecimal(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private Double getDouble(Struct struct, String fieldName) {
        if (struct == null || struct.schema().field(fieldName) == null) return null;
        Object value = struct.get(fieldName);
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).doubleValue();
        try {
            return Double.valueOf(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDate getLocalDate(Struct struct, String fieldName) {
        if (struct == null || struct.schema().field(fieldName) == null) return null;
        Object value = struct.get(fieldName);
        if (value == null) return null;
        // MySQL DATE columns arrive from Debezium as Integer (epoch days since 1970-01-01)
        if (value instanceof Integer || value instanceof Long) {
            return LocalDate.ofEpochDay(((Number) value).longValue());
        }
        try {
            return LocalDate.parse(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private String buildFullName(String firstName, String middleName, String lastName) {
        StringBuilder builder = new StringBuilder();
        if (firstName != null && !firstName.isBlank())   builder.append(firstName.trim()).append(" ");
        if (middleName != null && !middleName.isBlank()) builder.append(middleName.trim()).append(" ");
        if (lastName != null && !lastName.isBlank())     builder.append(lastName.trim());
        return builder.toString().trim();
    }

    // -------------------------------------------------------------------------
    // Map-based public entry points (used by retry/re-sync path)
    // -------------------------------------------------------------------------

    @Transactional
    public void processEmployeeDetailsFromMap(Map<String, Object> data) {
        String employeeId = getMapString(data, "employee_id");
        if (employeeId == null) return;

        Resource resource = resourceRepository.findByIdWithLock(employeeId)
                .orElseGet(Resource::new);
        boolean isNew = (resource.getVersion() == null);
        resource.setResourceId(employeeId);

        applyEmployeeDetailsMappingFromMap(resource, data);
        applyDerivedFieldsFromMap(resource, data);

        if (isNew) {
            if (resource.getFullName() == null || resource.getFullName().isBlank())
                resource.setFullName(employeeId);
            if (resource.getEmploymentType() == null)
                resource.setEmploymentType(EmploymentType.FULL_TIME);
            if (resource.getEmploymentStatus() == null)
                resource.setEmploymentStatus(EmploymentStatus.ACTIVE);
            if (resource.getActiveFlag() == null)
                resource.setActiveFlag(true);
            LocalDateTime srcCreatedAt = getMapLocalDateTime(data, "created_at");
            resource.setCreatedAt(srcCreatedAt != null ? srcCreatedAt : LocalDateTime.now());
        }

        LocalDateTime srcChangedAt = getMapLocalDateTime(data, "updated_at");
        resource.setChangedAt(srcChangedAt != null ? srcChangedAt : LocalDateTime.now());
        resourceRepository.save(resource);
        log.info("employee_details re-synced for resourceId={}", employeeId);
    }

    @Transactional
    public void processOfferDetailsFromMap(Map<String, Object> data) {
        String mail = getMapString(data, "mail");
        // employee_id may be present if the table has it, or injected by the resync service
        String employeeId = getMapString(data, "employee_id");
        if (mail == null && employeeId == null) return;

        Resource resource = null;
        if (employeeId != null) {
            resource = resourceRepository.findById(employeeId).orElse(null);
        }
        if (resource == null && mail != null) {
            resource = resourceRepository.findByEmail(mail).orElse(null);
        }
        if (resource == null) {
            String key = employeeId != null ? employeeId : mail;
            failureRecorder.recordFailure(
                    "EOS-offer_letter_details", key, "UPDATE",
                    "ResourceNotFound",
                    "Resource not found for offer letter (mail=" + mail
                            + ", employeeId=" + employeeId + ")",
                    null);
            return;
        }

        resource.setDesignation(getMapString(data, "designation"));
        resource.setAnnualCtc(getMapBigDecimal(data, "total_ctc"));
        resource.setCurrencyType(getMapString(data, "currency"));

        Long changedBy = getMapLong(data, "created_by");
        if (changedBy != null) resource.setChangedBy(changedBy);

        deriveHourlyCostRate(resource);
        LocalDateTime srcChangedAt = getMapLocalDateTime(data, "updated_at");
        resource.setChangedAt(srcChangedAt != null ? srcChangedAt : LocalDateTime.now());
        resourceRepository.save(resource);
        log.info("offer_letter_details re-synced for resourceId={}, mail={}", resource.getResourceId(), mail);
    }

    @Transactional
    public void processEmployeeExitFromMap(Map<String, Object> data) {
        String employeeId = getMapString(data, "employee_id");
        if (employeeId == null) return;

        Resource resource = resourceRepository.findById(employeeId).orElse(null);
        if (resource == null) {
            failureRecorder.recordFailure(
                    "EOS-employee_exit", employeeId, "UPDATE",
                    "ResourceNotFound",
                    "Resource not found for employeeId: " + employeeId + "; event may be out-of-order",
                    null);
            return;
        }

        resource.setDateOfExit(getMapLocalDate(data, "last_working_day"));
        resource.setNoticeStartDate(getMapLocalDate(data, "notice_start_date"));
        resource.setNoticeEndDate(getMapLocalDate(data, "notice_end_date"));

        Long changedBy = getMapLong(data, "created_by");
        if (changedBy != null) resource.setChangedBy(changedBy);

        deriveEmploymentStatusFromMap(resource, data);
        LocalDateTime srcChangedAt = getMapLocalDateTime(data, "updated_at");
        resource.setChangedAt(srcChangedAt != null ? srcChangedAt : LocalDateTime.now());
        resourceRepository.save(resource);
        log.info("employee_exit re-synced for resourceId={}", employeeId);
    }

    // -------------------------------------------------------------------------
    // Map-based private mapping helpers
    // -------------------------------------------------------------------------

    private void applyEmployeeDetailsMappingFromMap(Resource resource, Map<String, Object> data) {
        String fullName = buildFullName(
                getMapString(data, "first_name"),
                getMapString(data, "middle_name"),
                getMapString(data, "last_name"));
        if (fullName != null && !fullName.isEmpty()) resource.setFullName(fullName);

        resource.setEmail(getMapString(data, "work_email"));
        resource.setWorkingLocation(getMapString(data, "location"));
        resource.setExperiance(getMapDouble(data, "total_experience"));
        resource.setDateOfJoining(getMapLocalDate(data, "joining_date"));

        Long changedBy = getMapLong(data, "created_by");
        if (changedBy != null) resource.setChangedBy(changedBy);
    }

    private void applyDerivedFieldsFromMap(Resource resource, Map<String, Object> data) {
        String employmentType   = getMapString(data, "employment_type");
        String workMode         = getMapString(data, "work_mode");
        String employmentStatus = getMapString(data, "employment_status");

        if      ("Full-Time".equalsIgnoreCase(employmentType))  resource.setEmploymentType(EmploymentType.FULL_TIME);
        else if ("Part-Time".equalsIgnoreCase(employmentType))  resource.setEmploymentType(EmploymentType.PART_TIME);
        else if ("Intern".equalsIgnoreCase(employmentType))     resource.setEmploymentType(EmploymentType.INTERN);
        else if ("Contractor".equalsIgnoreCase(employmentType)) resource.setEmploymentType(EmploymentType.CONTRACTOR);
        else if ("Freelance".equalsIgnoreCase(employmentType))  resource.setEmploymentType(EmploymentType.FREELANCE);

        if      ("Office".equalsIgnoreCase(workMode)) resource.setWorkingMode(WorkingMode.OFFICE);
        else if ("Remote".equalsIgnoreCase(workMode)) resource.setWorkingMode(WorkingMode.REMOTE);
        else if ("Hybrid".equalsIgnoreCase(workMode)) resource.setWorkingMode(WorkingMode.HYBRID);

        if ("Active".equalsIgnoreCase(employmentStatus)) {
            resource.setEmploymentStatus(EmploymentStatus.ACTIVE);     resource.setActiveFlag(true);
        } else if ("Probation".equalsIgnoreCase(employmentStatus)) {
            resource.setEmploymentStatus(EmploymentStatus.PROBATION);  resource.setActiveFlag(true);
        } else if ("Resigned".equalsIgnoreCase(employmentStatus)) {
            resource.setEmploymentStatus(EmploymentStatus.RESIGNED);   resource.setActiveFlag(false);
        } else if ("Terminated".equalsIgnoreCase(employmentStatus)) {
            resource.setEmploymentStatus(EmploymentStatus.TERMINATED); resource.setActiveFlag(false);
        } else if ("Absconded".equalsIgnoreCase(employmentStatus)) {
            resource.setEmploymentStatus(EmploymentStatus.ABSCONDED);  resource.setActiveFlag(false);
        }
    }

    private void deriveEmploymentStatusFromMap(Resource resource, Map<String, Object> data) {
        String exitType = getMapString(data, "exit_type");
        String status   = getMapString(data, "status");

        if      ("Termination".equalsIgnoreCase(exitType)) { resource.setEmploymentStatus(EmploymentStatus.TERMINATED); resource.setActiveFlag(false); }
        else if ("Absconded".equalsIgnoreCase(exitType))   { resource.setEmploymentStatus(EmploymentStatus.ABSCONDED);  resource.setActiveFlag(false); }
        else if ("Resignation".equalsIgnoreCase(exitType)) { resource.setEmploymentStatus(EmploymentStatus.RESIGNED);   resource.setActiveFlag(true);  }

        if ("Completed".equalsIgnoreCase(status)) {
            resource.setEmploymentStatus(EmploymentStatus.EXITED); resource.setActiveFlag(false);
        } else {
            resource.setEmploymentStatus(EmploymentStatus.ON_NOTICE); resource.setActiveFlag(true);
        }
    }

    // -------------------------------------------------------------------------
    // Map extraction helpers
    // -------------------------------------------------------------------------

    private String getMapString(Map<String, Object> data, String field) {
        if (data == null) return null;
        Object v = data.get(field);
        return v != null ? v.toString() : null;
    }

    private Long getMapLong(Map<String, Object> data, String field) {
        if (data == null) return null;
        Object v = data.get(field);
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).longValue();
        try { return Long.parseLong(v.toString()); } catch (Exception e) { return null; }
    }

    private BigDecimal getMapBigDecimal(Map<String, Object> data, String field) {
        if (data == null) return null;
        Object v = data.get(field);
        if (v == null) return null;
        if (v instanceof BigDecimal) return (BigDecimal) v;
        try { return new BigDecimal(v.toString()); } catch (Exception e) { return null; }
    }

    private Double getMapDouble(Map<String, Object> data, String field) {
        if (data == null) return null;
        Object v = data.get(field);
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).doubleValue();
        try { return Double.valueOf(v.toString()); } catch (Exception e) { return null; }
    }

    private LocalDate getMapLocalDate(Map<String, Object> data, String field) {
        if (data == null) return null;
        Object v = data.get(field);
        if (v == null) return null;
        if (v instanceof java.sql.Date) return ((java.sql.Date) v).toLocalDate();
        if (v instanceof Number) return LocalDate.ofEpochDay(((Number) v).longValue());
        try { return LocalDate.parse(v.toString()); } catch (Exception e) { return null; }
    }

    // MySQL DATETIME arrives from Debezium as Long (epoch milliseconds).
    private LocalDateTime getLocalDateTime(Struct struct, String fieldName) {
        if (struct == null || struct.schema().field(fieldName) == null) return null;
        Object value = struct.get(fieldName);
        if (value == null) return null;
        if (value instanceof Long) {
            long ts = (Long) value;
            // Debezium encodes DATETIME as millis; guard against microsecond range
            if (ts > 1_000_000_000_000_000L)
                return LocalDateTime.ofInstant(Instant.ofEpochSecond(ts / 1_000_000, (ts % 1_000_000) * 1000), ZoneId.systemDefault());
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneId.systemDefault());
        }
        try { return LocalDateTime.parse(value.toString()); } catch (Exception e) { return null; }
    }

    // JDBC queryForMap returns java.sql.Timestamp for DATETIME/TIMESTAMP columns.
    private LocalDateTime getMapLocalDateTime(Map<String, Object> data, String field) {
        if (data == null) return null;
        Object v = data.get(field);
        if (v == null) return null;
        if (v instanceof java.sql.Timestamp) return ((java.sql.Timestamp) v).toLocalDateTime();
        if (v instanceof LocalDateTime) return (LocalDateTime) v;
        if (v instanceof Long) return LocalDateTime.ofInstant(Instant.ofEpochMilli((Long) v), ZoneId.systemDefault());
        try { return LocalDateTime.parse(v.toString()); } catch (Exception e) { return null; }
    }

    // Resolves employee_id from offer_letter_details.user_uuid via employee_details join.
    private String resolveEmployeeIdByUserUuid(String userUuid) {
        if (userUuid == null) return null;
        try {
            return eosJdbcTemplate.queryForObject(
                    "SELECT employee_id FROM employee_details WHERE user_uuid = ?",
                    String.class, userUuid);
        } catch (EmptyResultDataAccessException e) {
            return null;
        } catch (Exception e) {
            log.warn("Cannot resolve employee_id for user_uuid={}: {}", userUuid, e.getMessage());
            return null;
        }
    }
}
