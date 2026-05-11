package com.cdc.service;

import com.cdc.failure.FailureRecorder;
import com.entity.resource_entities.Resource;
import com.entity_enums.resource_enums.EmploymentStatus;
import com.entity_enums.resource_enums.EmploymentType;
import com.entity_enums.resource_enums.WorkingMode;
import com.repo.resource_repo.ResourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.connect.data.Struct;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class EosResourceSyncService {

    private final ResourceRepository resourceRepository;
    private final FailureRecorder failureRecorder;

    // -------------------------------------------------------------------------
    // Public entry points (one per EOS table)
    // -------------------------------------------------------------------------

    public void processEmployeeDetails(Struct after) {

        String employeeId = getString(after, "employee_id");
        if (employeeId == null) return;

        // Use pessimistic lock when the resource already exists to prevent
        // concurrent updates in multi-instance deployments.
        Resource resource = resourceRepository.findByIdWithLock(employeeId)
                .orElseGet(Resource::new);

        resource.setResourceId(employeeId);

        applyEmployeeDetailsMapping(resource, after);   // direct field copy
        applyDerivedFields(resource, after);            // employmentType, workingMode, employmentStatus, activeFlag

        resource.setChangedAt(LocalDateTime.now());

        resourceRepository.save(resource);
        log.info("employee_details synced successfully for resourceId={}", employeeId);
    }

    public void processOfferDetails(Struct after) {

        String email = getString(after, "mail");
        if (email == null) return;

        Resource resource = resourceRepository.findByEmail(email).orElse(null);
        if (resource == null) {
            // Out-of-order event: offer arrived before employee_details was processed.
            // Record for retry so it is re-attempted once the resource exists.
            failureRecorder.recordFailure(
                    "EOS-offer_letter_details", email, "UPDATE",
                    "ResourceNotFound",
                    "Resource not found for email: " + email + "; event may be out-of-order",
                    null);
            return;
        }

        resource.setDesignation(getString(after, "designation"));
        resource.setAnnualCtc(getBigDecimal(after, "total_ctc"));
        resource.setCurrencyType(getString(after, "currency"));

        Long changedBy = getLong(after, "created_by");
        if (changedBy != null) resource.setChangedBy(changedBy);

        deriveHourlyCostRate(resource);

        resource.setChangedAt(LocalDateTime.now());

        resourceRepository.save(resource);
        log.info("offer_letter_details enriched successfully for email={}", email);
    }

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

        resource.setChangedAt(LocalDateTime.now());

        resourceRepository.save(resource);
        log.info("employee_exit processed successfully for resourceId={}", employeeId);
    }

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
}
