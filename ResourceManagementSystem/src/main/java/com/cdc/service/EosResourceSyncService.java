package com.cdc.service;

import com.cdc.model.CdcProcessingOutcome;
import com.cdc.protection.StaleEventProtectionService;
import com.cdc.util.CdcUtcSupport;
import com.entity.resource_entities.Resource;
import com.entity_enums.ledger_enums.EventStatus;
import com.entity_enums.resource_enums.EmploymentStatus;
import com.entity_enums.resource_enums.EmploymentType;
import com.entity_enums.resource_enums.WorkingMode;
import com.repo.ledger_repo.LedgerEventLogRepository;
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
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@Slf4j
public class EosResourceSyncService {

    private final ResourceRepository resourceRepository;
    private final JdbcTemplate eosJdbcTemplate;
    private final StaleEventProtectionService staleEventProtectionService;
    private final CdcUtcSupport cdcUtcSupport;
    private final LedgerEventLogRepository ledgerEventLogRepository;

    public EosResourceSyncService(
            ResourceRepository resourceRepository,
            @Qualifier("eosJdbcTemplate") JdbcTemplate eosJdbcTemplate,
            StaleEventProtectionService staleEventProtectionService,
            CdcUtcSupport cdcUtcSupport,
            LedgerEventLogRepository ledgerEventLogRepository) {
        this.resourceRepository = resourceRepository;
        this.eosJdbcTemplate = eosJdbcTemplate;
        this.staleEventProtectionService = staleEventProtectionService;
        this.cdcUtcSupport = cdcUtcSupport;
        this.ledgerEventLogRepository = ledgerEventLogRepository;
    }

    @Transactional
    public void processEmployeeDetails(Struct after) {
        processEmployeeDetailsFromMap(structToMap(after), getLocalDateTime(after, "updated_at") != null
                ? getLocalDateTime(after, "updated_at").toInstant(ZoneOffset.UTC) : null);
    }

    @Transactional
    public void processOfferDetails(Struct after) {
        processOfferDetailsFromMap(structToMap(after), getLocalDateTime(after, "updated_at") != null
                ? getLocalDateTime(after, "updated_at").toInstant(ZoneOffset.UTC) : null);
    }

    @Transactional
    public void processEmployeeExit(Struct after) {
        processEmployeeExitFromMap(structToMap(after), getLocalDateTime(after, "updated_at") != null
                ? getLocalDateTime(after, "updated_at").toInstant(ZoneOffset.UTC) : null);
    }

    @Transactional
    public void handleDelete(String tableName, Struct before) {
        handleDeleteFromMap(tableName, structToMap(before), cdcUtcSupport.extractInstant(before != null ? before.get("updated_at") : null));
    }

    @Transactional
    public void processEmployeeDetailsFromMap(Map<String, Object> data) {
        processEmployeeDetailsFromMap(data, cdcUtcSupport.extractSourceTimestamp(null, data));
    }

    @Transactional
    public void processEmployeeDetailsFromMap(Map<String, Object> data, Instant sourceTimestamp) {
        String employeeId = getMapString(data, "employee_id");
        if (employeeId == null) {
            return;
        }

        Resource resource = resourceRepository
                .findByIdWithLock(employeeId)
                .orElseGet(() -> {

                    String workEmail = getMapString(data, "work_email");

                    if (workEmail != null) {

                        Optional<Resource> existingByEmail =
                                resourceRepository.findByEmailIgnoreCase(workEmail);

                        if (existingByEmail.isPresent()) {

                            Resource existing = existingByEmail.get();

                            log.error(
                                    "Duplicate business identity detected. Existing resourceId={}, incoming employeeId={}, email={}",
                                    existing.getResourceId(),
                                    employeeId,
                                    workEmail
                            );

                            // IMPORTANT:
                            // Reuse existing entity WITHOUT changing primary key
                            return existing;
                        }
                    }

                    return new Resource();
                });
        boolean isNew = resource.getVersion() == null;
        if (!isNew && staleEventProtectionService.isStaleEvent(resource.getChangedAt(), sourceTimestamp, employeeId)) {
            log.warn("Skipping stale EOS employee_details event for resourceId={}", employeeId);
            return;
        }

        if (isNew) {
            resource.setResourceId(employeeId);
        }
        applyEmployeeDetailsMappingFromMap(resource, data);
        applyDerivedFieldsFromMap(resource, data);

        if (isNew) {
            applyCreationDefaults(resource, employeeId, getMapLocalDateTime(data, "created_at"));
        }

        resource.setChangedAt(resolveChangedAt(data, sourceTimestamp));
        resourceRepository.save(resource);
        releaseWaitingOfferEvents(employeeId, getMapString(data, "work_email"));
        log.info("employee_details synced successfully for resourceId={}", employeeId);
    }

    @Transactional
    public void processOfferDetailsFromMap(Map<String, Object> data) {
        processOfferDetailsFromMap(data, cdcUtcSupport.extractSourceTimestamp(null, data));
    }

    @Transactional
    public CdcProcessingOutcome processOfferDetailsFromMap(Map<String, Object> data, Instant sourceTimestamp) {
        return processOfferDetailsFromMap(data, sourceTimestamp, false);
    }

    @Transactional
    public CdcProcessingOutcome processOfferDetailsFromMap(Map<String, Object> data, Instant sourceTimestamp, boolean dependencyReplay) {
        String mail = getMapString(data, "mail");
        String employeeId = getMapString(data, "employee_id");
        String offerStatus = normalizeOfferStatus(getMapString(data, "status"));
        if (mail == null && employeeId == null) {
            return CdcProcessingOutcome.success();
        }

        if (employeeId == null) {
            employeeId = resolveEmployeeIdByUserUuid(getMapString(data, "user_uuid"));
        }
        if (employeeId == null) {
            return orchestrateMissingOfferDependency(mail, null, offerStatus, "employee_id could not be resolved from offer_letter_details payload");
        }

        Resource resource = resourceRepository.findByIdWithLock(employeeId).orElse(null);
        if (resource == null) {
            return orchestrateMissingOfferDependency(mail, employeeId, offerStatus,
                    "Resource not found for offer letter enrichment");
        }

        if (isNonActionableOfferStatus(offerStatus)) {
            log.info("Skipping offer_letter_details enrichment for resourceId={}, mail={}, status={} because lifecycle state is non-actionable",
                    employeeId, mail, offerStatus);
            return CdcProcessingOutcome.cancelled(
                    "offer_letter_details lifecycle state is non-actionable for enrichment",
                    "NON_ACTIONABLE_STATUS",
                    offerStatus
            );
        }

        boolean staleOfferEvent = staleEventProtectionService.isStaleEvent(resource.getChangedAt(), sourceTimestamp, employeeId);
        boolean allowDeferredReplay = dependencyReplay && staleOfferEvent && canApplyDeferredOfferEnrichment(resource);
        if (staleOfferEvent && !allowDeferredReplay) {
            log.warn("Skipping stale EOS offer_letter_details event for resourceId={}, mail={}, status={}, dependencyReplay={}",
                    employeeId, mail, offerStatus, dependencyReplay);
            return CdcProcessingOutcome.success();
        }
        if (allowDeferredReplay) {
            log.info("Applying deferred offer_letter_details replay for resourceId={}, mail={}, status={} despite older source timestamp because enrichment fields are still incomplete",
                    employeeId, mail, offerStatus);
        }

        resource.setDesignation(getMapString(data, "designation"));
        resource.setAnnualCtc(getMapBigDecimal(data, "total_ctc"));
        resource.setCurrencyType(getMapString(data, "currency"));

        Long changedBy = getMapLong(data, "created_by");
        if (changedBy != null) {
            resource.setChangedBy(changedBy);
        }

        deriveHourlyCostRate(resource);
        resource.setChangedAt(resolveOfferChangedAt(resource, data, sourceTimestamp, allowDeferredReplay));
        resourceRepository.save(resource);
        log.info("offer_letter_details enriched for resourceId={}, mail={}, status={}, dependencyReplay={}",
                resource.getResourceId(), mail, offerStatus, dependencyReplay);
        return CdcProcessingOutcome.success();
    }

    @Transactional
    public void processEmployeeExitFromMap(Map<String, Object> data) {
        processEmployeeExitFromMap(data, cdcUtcSupport.extractSourceTimestamp(null, data));
    }

    @Transactional
    public void processEmployeeExitFromMap(Map<String, Object> data, Instant sourceTimestamp) {
        String employeeId = getMapString(data, "employee_id");
        if (employeeId == null) {
            return;
        }

        Resource resource = resourceRepository.findByIdWithLock(employeeId).orElse(null);
        if (resource == null) {
            throw new IllegalStateException("Resource not found for employee exit: " + employeeId);
        }
        if (staleEventProtectionService.isStaleEvent(resource.getChangedAt(), sourceTimestamp, employeeId)) {
            log.warn("Skipping stale EOS employee_exit event for resourceId={}", employeeId);
            return;
        }

        resource.setDateOfExit(getMapLocalDate(data, "last_working_day"));
        resource.setNoticeStartDate(getMapLocalDate(data, "notice_start_date"));
        resource.setNoticeEndDate(getMapLocalDate(data, "notice_end_date"));

        Long changedBy = getMapLong(data, "created_by");
        if (changedBy != null) {
            resource.setChangedBy(changedBy);
        }

        deriveEmploymentStatusFromMap(resource, data);
        resource.setChangedAt(resolveChangedAt(data, sourceTimestamp));
        resourceRepository.save(resource);
        log.info("employee_exit processed successfully for resourceId={}", employeeId);
    }

    @Transactional
    public void handleDeleteFromMap(String tableName, Map<String, Object> before, Instant sourceTimestamp) {
        if (before == null || !"employee_details".equals(tableName)) {
            return;
        }

        String employeeId = getMapString(before, "employee_id");
        if (employeeId == null) {
            return;
        }

        Resource resource = resourceRepository.findByIdWithLock(employeeId).orElse(null);
        if (resource == null) {
            return;
        }
        if (staleEventProtectionService.isStaleEvent(resource.getChangedAt(), sourceTimestamp, employeeId)) {
            log.warn("Skipping stale EOS delete event for resourceId={}", employeeId);
            return;
        }

        resource.setActiveFlag(false);
        resource.setEmploymentStatus(EmploymentStatus.EXITED);
        resource.setDateOfExit(LocalDate.now(ZoneOffset.UTC));

        Long changedBy = getMapLong(before, "created_by");
        if (changedBy != null) {
            resource.setChangedBy(changedBy);
        }

        resource.setChangedAt(cdcUtcSupport.utcDateTime(sourceTimestamp != null ? sourceTimestamp : cdcUtcSupport.now()));
        resourceRepository.save(resource);
        log.info("Soft delete completed for resourceId={}", employeeId);
    }

    private void applyCreationDefaults(Resource resource, String employeeId, LocalDateTime createdAt) {
        if (resource.getFullName() == null || resource.getFullName().isBlank()) {
            resource.setFullName(employeeId);
        }
        if (resource.getEmploymentType() == null) {
            resource.setEmploymentType(EmploymentType.FULL_TIME);
        }
        if (resource.getEmploymentStatus() == null) {
            resource.setEmploymentStatus(EmploymentStatus.ACTIVE);
        }
        if (resource.getActiveFlag() == null) {
            resource.setActiveFlag(true);
        }
        resource.setCreatedAt(createdAt != null ? createdAt : cdcUtcSupport.utcDateTime(cdcUtcSupport.now()));
    }

    private LocalDateTime resolveChangedAt(Map<String, Object> data, Instant sourceTimestamp) {
        LocalDateTime srcChangedAt = getMapLocalDateTime(data, "updated_at");
        if (srcChangedAt != null) {
            return srcChangedAt;
        }
        return cdcUtcSupport.utcDateTime(sourceTimestamp != null ? sourceTimestamp : cdcUtcSupport.now());
    }

    private void applyEmployeeDetailsMappingFromMap(Resource resource, Map<String, Object> data) {
        String fullName = buildFullName(
                getMapString(data, "first_name"),
                getMapString(data, "middle_name"),
                getMapString(data, "last_name"));
        if (fullName != null && !fullName.isEmpty()) {
            resource.setFullName(fullName);
        }

        resource.setEmail(getMapString(data, "work_email"));
        resource.setWorkingLocation(getMapString(data, "location"));
        resource.setExperiance(getMapDouble(data, "total_experience"));
        resource.setDateOfJoining(getMapLocalDate(data, "joining_date"));

        Long changedBy = getMapLong(data, "created_by");
        if (changedBy != null) {
            resource.setChangedBy(changedBy);
        }
    }

    private void applyDerivedFieldsFromMap(Resource resource, Map<String, Object> data) {
        String employmentType = getMapString(data, "employment_type");
        String workMode = getMapString(data, "work_mode");
        String employmentStatus = getMapString(data, "employment_status");

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

    private void deriveEmploymentStatusFromMap(Resource resource, Map<String, Object> data) {
        String exitType = getMapString(data, "exit_type");
        String status = getMapString(data, "status");

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
        if (annualCtc == null || annualCtc.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        BigDecimal hourlyRate = annualCtc
                .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(22), 2, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(8), 2, RoundingMode.HALF_UP);

        resource.setHourlyCostRate(hourlyRate);
    }

    private LocalDateTime resolveOfferChangedAt(Resource resource,
                                                Map<String, Object> data,
                                                Instant sourceTimestamp,
                                                boolean allowDeferredReplay) {
        LocalDateTime resolved = resolveChangedAt(data, sourceTimestamp);
        if (!allowDeferredReplay || resolved == null || resource.getChangedAt() == null) {
            return resolved;
        }
        return resolved.isBefore(resource.getChangedAt()) ? resource.getChangedAt() : resolved;
    }

    private boolean canApplyDeferredOfferEnrichment(Resource resource) {
        return isBlank(resource.getDesignation())
                || resource.getAnnualCtc() == null
                || isBlank(resource.getCurrencyType())
                || resource.getHourlyCostRate() == null;
    }

    private CdcProcessingOutcome orchestrateMissingOfferDependency(String mail,
                                                                   String employeeId,
                                                                   String offerStatus,
                                                                   String detail) {
        String entityRef = employeeId != null ? employeeId : mail;
        if (isWaitingOfferStatus(offerStatus)) {
            String message = "offer_letter_details deferred because employee lifecycle dependency is not ready"
                    + " [entityRef=" + entityRef + ", status=" + offerStatus + ", detail=" + detail + "]";
            log.info(message);
            return CdcProcessingOutcome.waiting(
                    message,
                    "EMPLOYEE_DETAILS_PENDING",
                    offerStatus
            );
        }

        if (isCancelledOfferStatus(offerStatus)) {
            String reasonCode = "Rejected".equalsIgnoreCase(offerStatus)
                    ? "OFFER_REJECTED"
                    : "DRAFT_OFFER_NO_ACTION";
            String message = "offer_letter_details cancelled because lifecycle state does not require enrichment"
                    + " [entityRef=" + entityRef + ", status=" + offerStatus + ", detail=" + detail + "]";
            log.info(message);
            return CdcProcessingOutcome.cancelled(message, reasonCode, offerStatus);
        }

        String message = "offer_letter_details missing authoritative employee dependency for a retryable lifecycle state"
                + " [entityRef=" + entityRef + ", status=" + offerStatus + ", detail=" + detail + "]";
        log.warn(message);
        throw new IllegalStateException(message);
    }

    private void releaseWaitingOfferEvents(String employeeId, String workEmail) {
        List<String> entityIds = new ArrayList<>(deduplicateEntityIds(employeeId, workEmail));
        if (entityIds.isEmpty()) {
            return;
        }

        LocalDateTime now = cdcUtcSupport.utcDateTime(cdcUtcSupport.now());
        int released = ledgerEventLogRepository.releaseWaitingDependencyEvents(
                "EOS",
                "offer_letter_details",
                EventStatus.WAITING_FOR_DEPENDENCY,
                EventStatus.RETRY_SCHEDULED,
                entityIds,
                now,
                now
        );
        if (released > 0) {
            log.info("Released {} waiting offer_letter_details event(s) after employee_details sync for employeeId={}, workEmail={}",
                    released, employeeId, workEmail);
        }
    }

    private Set<String> deduplicateEntityIds(String employeeId, String workEmail) {
        Set<String> entityIds = new LinkedHashSet<>();
        if (!isBlank(employeeId)) {
            entityIds.add(employeeId);
        }
        if (!isBlank(workEmail)) {
            entityIds.add(workEmail);
        }
        return entityIds;
    }

    private boolean isWaitingOfferStatus(String offerStatus) {
        if (offerStatus == null) {
            return false;
        }
        return "Offered".equalsIgnoreCase(offerStatus)
                || "Accepted".equalsIgnoreCase(offerStatus)
                || "Submitted".equalsIgnoreCase(offerStatus)
                || "Verified".equalsIgnoreCase(offerStatus)
                || "Joining".equalsIgnoreCase(offerStatus)
                || "Joining Pending".equalsIgnoreCase(offerStatus)
                || "Rescheduled".equalsIgnoreCase(offerStatus);
    }

    private boolean isCancelledOfferStatus(String offerStatus) {
        return "Rejected".equalsIgnoreCase(offerStatus)
                || "Created".equalsIgnoreCase(offerStatus);
    }

    private boolean isNonActionableOfferStatus(String offerStatus) {
        return isCancelledOfferStatus(offerStatus);
    }

    private String normalizeOfferStatus(String offerStatus) {
        return offerStatus != null ? offerStatus.trim() : null;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String getMapString(Map<String, Object> data, String field) {
        if (data == null) {
            return null;
        }
        Object value = data.get(field);
        return value != null ? value.toString() : null;
    }

    private Long getMapLong(Map<String, Object> data, String field) {
        if (data == null) {
            return null;
        }
        Object value = data.get(field);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (Exception ex) {
            return null;
        }
    }

    private BigDecimal getMapBigDecimal(Map<String, Object> data, String field) {
        if (data == null) {
            return null;
        }
        Object value = data.get(field);
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        try {
            return new BigDecimal(value.toString());
        } catch (Exception ex) {
            return null;
        }
    }

    private Double getMapDouble(Map<String, Object> data, String field) {
        if (data == null) {
            return null;
        }
        Object value = data.get(field);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.valueOf(value.toString());
        } catch (Exception ex) {
            return null;
        }
    }

    private LocalDate getMapLocalDate(Map<String, Object> data, String field) {
        if (data == null) {
            return null;
        }
        Object value = data.get(field);
        if (value == null) {
            return null;
        }
        if (value instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        if (value instanceof Number number) {
            return LocalDate.ofEpochDay(number.longValue());
        }
        try {
            return LocalDate.parse(value.toString());
        } catch (Exception ex) {
            return null;
        }
    }

    private LocalDateTime getMapLocalDateTime(Map<String, Object> data, String field) {
        if (data == null) {
            return null;
        }
        Object value = data.get(field);
        if (value == null) {
            return null;
        }
        if (value instanceof java.sql.Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        Instant instant = cdcUtcSupport.extractInstant(value);
        if (instant != null) {
            return cdcUtcSupport.utcDateTime(instant);
        }
        try {
            return LocalDateTime.parse(value.toString());
        } catch (Exception ex) {
            return null;
        }
    }

    private LocalDateTime getLocalDateTime(Struct struct, String fieldName) {
        if (struct == null || struct.schema().field(fieldName) == null) {
            return null;
        }
        Object value = struct.get(fieldName);
        Instant instant = cdcUtcSupport.extractInstant(value);
        return instant != null ? cdcUtcSupport.utcDateTime(instant) : null;
    }

    private String buildFullName(String firstName, String middleName, String lastName) {
        StringBuilder builder = new StringBuilder();
        if (firstName != null && !firstName.isBlank()) {
            builder.append(firstName.trim()).append(" ");
        }
        if (middleName != null && !middleName.isBlank()) {
            builder.append(middleName.trim()).append(" ");
        }
        if (lastName != null && !lastName.isBlank()) {
            builder.append(lastName.trim());
        }
        return builder.toString().trim();
    }

    private String resolveEmployeeIdByUserUuid(String userUuid) {
        if (userUuid == null) {
            return null;
        }
        try {
            return eosJdbcTemplate.queryForObject(
                    "SELECT employee_id FROM employee_details WHERE user_uuid = ?",
                    String.class,
                    userUuid
            );
        } catch (EmptyResultDataAccessException ex) {
            return null;
        } catch (Exception ex) {
            log.warn("Cannot resolve employee_id for user_uuid={}: {}", userUuid, ex.getMessage());
            return null;
        }
    }

    private Map<String, Object> structToMap(Struct struct) {
        if (struct == null) {
            return null;
        }
        java.util.LinkedHashMap<String, Object> map = new java.util.LinkedHashMap<>();
        for (org.apache.kafka.connect.data.Field field : struct.schema().fields()) {
            map.put(field.name(), struct.get(field));
        }
        return map;
    }
}
