package com.cdc.derivation;

import com.entity_enums.resource_enums.EmploymentType;
import com.entity_enums.resource_enums.EmploymentStatus;
import com.entity_enums.resource_enums.WorkingMode;
import com.entity.resource_entities.Resource;
import io.debezium.data.Envelope;
import org.apache.kafka.connect.data.Struct;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;

/**
 * Service for deriving RMS Resource fields from EOS source data.
 * 
 * This service handles the DERIVED FIELD CALCULATION phase of EOS CDC processing.
 * It calculates RMS business fields using EOS source columns and internal business rules.
 * 
 * Architecture:
 * PHASE 1: Direct field mapping (handled by EosCdcMappingRegistry)
 * PHASE 2: Derived field calculation (handled by this service)
 */
@Service
public class ResourceDerivationService {

    /**
     * Populate all derived fields for a Resource entity using EOS source data.
     * 
     * @param resource The RMS Resource entity to populate
     * @param eosPayload The EOS Debezium payload containing source data
     */
    public void populateDerivedFields(Resource resource, Struct eosPayload) {
        if (resource == null || eosPayload == null) {
            return;
        }

        // Derive employment status based on EOS business conditions
        deriveEmploymentStatus(resource, eosPayload);
        
        // Derive working mode from EOS work mode and location data
        deriveWorkingMode(resource, eosPayload);
        
        // Derive employment type from EOS employment categories
        deriveEmploymentType(resource, eosPayload);
        
        // Calculate experience from EOS dates
        deriveExperience(resource, eosPayload);
        
        // Derive active flag from EOS business state
        deriveActiveFlag(resource, eosPayload);
        
        // Calculate derived cost values
        deriveCostFields(resource, eosPayload);
        
        // Normalize and validate derived values
        normalizeDerivedFields(resource);
    }

    /**
     * Derive employment status using EOS business rules.
     * 
     * Business Logic:
     * - If date_of_exit exists → EXITED
     * - If notice period active → ON_NOTICE  
     * - If active_flag = true → ACTIVE
     * - Otherwise → INACTIVE
     */
    private void deriveEmploymentStatus(Resource resource, Struct eosPayload) {
        LocalDate dateOfExit = extractLocalDate(eosPayload, "date_of_exit");
        LocalDate noticeStartDate = extractLocalDate(eosPayload, "notice_start_date");
        LocalDate noticeEndDate = extractLocalDate(eosPayload, "notice_end_date");
        Boolean activeFlag = extractBoolean(eosPayload, "active_flag");
        
        LocalDate today = LocalDate.now();
        
        if (dateOfExit != null && !dateOfExit.isAfter(today)) {
            resource.setEmploymentStatus(EmploymentStatus.EXITED);
        } else if (isNoticePeriodActive(noticeStartDate, noticeEndDate, today)) {
            resource.setEmploymentStatus(EmploymentStatus.ON_NOTICE);
        } else if (Boolean.TRUE.equals(activeFlag)) {
            resource.setEmploymentStatus(EmploymentStatus.ACTIVE);
        } else {
            resource.setEmploymentStatus(EmploymentStatus.EXITED);
        }
    }

    /**
     * Derive working mode from EOS work mode codes and location data.
     * 
     * CONTRACT COMPLIANCE: Maps EOS business values to RMS enums:
     * - REMOTE → REMOTE (contract expects WFH, enum has REMOTE)
     * - OFFICE → OFFICE (contract expects WFO, enum has OFFICE)
     * - HYBRID → HYBRID
     * 
     * Note: Using actual enum values (REMOTE, OFFICE, HYBRID) instead of contract-specified (WFH, WFO, HYBRID)
     */
    private void deriveWorkingMode(Resource resource, Struct eosPayload) {
        String workModeCode = extractString(eosPayload, "work_mode_code");
        String locationType = extractString(eosPayload, "location_type");
        Boolean remoteFlag = extractBoolean(eosPayload, "remote_flag");
        
        // Primary mapping from work_mode_code using actual enum values
        if (workModeCode != null) {
            switch (workModeCode.toUpperCase()) {
                case "WFO", "OFFICE", "ONSITE" -> {
                    resource.setWorkingMode(WorkingMode.OFFICE);
                    return;
                }
                case "WFH", "REMOTE", "HOME" -> {
                    resource.setWorkingMode(WorkingMode.REMOTE);
                    return;
                }
                case "HYBRID", "FLEXIBLE", "MIXED" -> {
                    resource.setWorkingMode(WorkingMode.HYBRID);
                    return;
                }
            }
        }
        
        // Fallback logic using location_type and remote_flag
        if (Boolean.TRUE.equals(remoteFlag) || "REMOTE".equalsIgnoreCase(locationType)) {
            resource.setWorkingMode(WorkingMode.REMOTE);
        } else if ("OFFICE".equalsIgnoreCase(locationType) || Boolean.FALSE.equals(remoteFlag)) {
            resource.setWorkingMode(WorkingMode.OFFICE);
        } else {
            // Default to HYBRID if unclear
            resource.setWorkingMode(WorkingMode.HYBRID);
        }
    }

    /**
     * Derive employment type from EOS employment categories and payroll types.
     * 
     * CONTRACT COMPLIANCE: Maps EOS business values to RMS enums per CDC contract:
     * - PERMANENT → FULL_TIME
     * - CONTRACT → CONTRACTOR
     * - INTERN → INTERN
     * - CONSULTANT → CONTRACTOR
     */
    private void deriveEmploymentType(Resource resource, Struct eosPayload) {
        String employeeCategory = extractString(eosPayload, "employee_category");
        String payrollType = extractString(eosPayload, "payroll_type");
        String contractType = extractString(eosPayload, "contract_type");
        
        // Primary mapping from employee_category per contract
        if (employeeCategory != null) {
            switch (employeeCategory.toUpperCase()) {
                case "PERMANENT", "FULL_TIME", "REGULAR", "STAFF" -> {
                    resource.setEmploymentType(EmploymentType.FULL_TIME);
                    return;
                }
                case "CONTRACT", "CONTRACTOR", "VENDOR" -> {
                    resource.setEmploymentType(EmploymentType.CONTRACTOR);
                    return;
                }
                case "INTERN", "TRAINEE", "APPRENTICE", "STUDENT" -> {
                    resource.setEmploymentType(EmploymentType.INTERN);
                    return;
                }
                case "CONSULTANT", "FREELANCE" -> {
                    resource.setEmploymentType(EmploymentType.CONTRACTOR);
                    return;
                }
                case "PART_TIME", "PARTTIME", "CASUAL" -> {
                    resource.setEmploymentType(EmploymentType.PART_TIME);
                    return;
                }
            }
        }
        
        // Fallback logic using payroll_type and contract_type
        if ("CONTRACT".equalsIgnoreCase(payrollType) || "CONTRACT".equalsIgnoreCase(contractType)) {
            resource.setEmploymentType(EmploymentType.CONTRACTOR);
        } else if ("PART_TIME".equalsIgnoreCase(payrollType)) {
            resource.setEmploymentType(EmploymentType.PART_TIME);
        } else {
            // Default to FULL_TIME
            resource.setEmploymentType(EmploymentType.FULL_TIME);
        }
    }

    /**
     * Calculate experience from EOS dates.
     * 
     * Uses date_of_joining and current date (or exit date if employee exited).
     * Returns experience in years with decimal precision.
     */
    private void deriveExperience(Resource resource, Struct eosPayload) {
        LocalDate dateOfJoining = extractLocalDate(eosPayload, "date_of_joining");
        LocalDate dateOfExit = extractLocalDate(eosPayload, "date_of_exit");
        
        if (dateOfJoining == null) {
            resource.setExperiance(0.0);
            return;
        }
        
        LocalDate endDate = dateOfExit != null ? dateOfExit : LocalDate.now();
        
        // Calculate experience in years with decimal precision
        double experienceInYears = calculateYearsBetween(dateOfJoining, endDate);
        
        // Round to 2 decimal places for consistency
        resource.setExperiance(Math.round(experienceInYears * 100.0) / 100.0);
    }

    /**
     * Derive active flag from EOS business state and conditions.
     * 
     * Business Logic:
     * - Employee is active if not exited and active_flag = true
     * - Consider notice period, deactivation flags, exit status
     */
    private void deriveActiveFlag(Resource resource, Struct eosPayload) {
        LocalDate dateOfExit = extractLocalDate(eosPayload, "date_of_exit");
        Boolean activeFlag = extractBoolean(eosPayload, "active_flag");
        String deactivationReason = extractString(eosPayload, "deactivation_reason");
        
        LocalDate today = LocalDate.now();
        
        // Employee is inactive if exited
        if (dateOfExit != null && !dateOfExit.isAfter(today)) {
            resource.setActiveFlag(false);
            return;
        }
        
        // Check explicit deactivation
        if (deactivationReason != null && !deactivationReason.trim().isEmpty()) {
            resource.setActiveFlag(false);
            return;
        }
        
        // Use EOS active flag if available
        if (activeFlag != null) {
            resource.setActiveFlag(activeFlag);
        } else {
            // Default to true if no explicit flags
            resource.setActiveFlag(true);
        }
    }

    /**
     * Calculate derived cost fields from EOS financial data.
     * 
     * Calculates hourly cost from annual CTC if not provided.
     * Applies business rules for cost calculations.
     */
    private void deriveCostFields(Resource resource, Struct eosPayload) {
        BigDecimal annualCtc = extractBigDecimal(eosPayload, "annual_ctc");
        BigDecimal hourlyCostRate = extractBigDecimal(eosPayload, "hourly_cost_rate");
        
        // If hourly cost is not provided, calculate from annual CTC
        if (hourlyCostRate == null && annualCtc != null && annualCtc.compareTo(BigDecimal.ZERO) > 0) {
            // Business assumption: 2080 working hours per year (40 hours * 52 weeks)
            BigDecimal workingHoursPerYear = new BigDecimal("2080");
            BigDecimal calculatedHourlyRate = annualCtc.divide(workingHoursPerYear, 2, RoundingMode.HALF_UP);
            resource.setHourlyCostRate(calculatedHourlyRate);
        } else if (hourlyCostRate != null) {
            resource.setHourlyCostRate(hourlyCostRate);
        }
    }

    /**
     * Normalize and validate only derived/calculated field values.
     * 
     * Preserves EOS read-only source fields and null values from PMS-RMS implementation.
     * Only normalizes invalid negative values, keeps null as null.
     */
    private void normalizeDerivedFields(Resource resource) {
        // Only normalize derived/calculated fields, not EOS source fields
        // EOS fields are read-only and should be preserved as-is
        // PMS-RMS implementation: keep null values as null
        
        // Ensure derived experience is not negative (calculated field)
        // Keep null as null, only fix negative values
        if (resource.getExperiance() != null && resource.getExperiance() < 0) {
            resource.setExperiance(0.0);
        }
        
        // Ensure derived hourly cost rate is not negative (calculated from annual CTC)
        // Note: annualCtc comes from EOS and should not be normalized
        // Keep null as null, only fix negative values
        if (resource.getHourlyCostRate() != null && resource.getHourlyCostRate().compareTo(BigDecimal.ZERO) < 0) {
            resource.setHourlyCostRate(BigDecimal.ZERO);
        }
        
        // PMS-RMS implementation: keep null enum values as null
        // Do not set default values for derived enums
        // These will be set by business logic during derivation if needed
    }

    // Helper methods for extracting values from EOS payload

    private String extractString(Struct struct, String fieldName) {
        if (struct == null) return null;
        Object value = struct.get(fieldName);
        return value != null ? value.toString() : null;
    }

    private Boolean extractBoolean(Struct struct, String fieldName) {
        if (struct == null) return null;
        Object value = struct.get(fieldName);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            String strValue = ((String) value).toLowerCase();
            return "true".equals(strValue) || "1".equals(strValue) || "yes".equals(strValue);
        }
        if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        }
        return null;
    }

    private LocalDate extractLocalDate(Struct struct, String fieldName) {
        if (struct == null) return null;
        Object value = struct.get(fieldName);
        
        if (value == null) return null;
        
        if (value instanceof LocalDate) {
            return (LocalDate) value;
        }
        
        if (value instanceof String) {
            try {
                return LocalDate.parse(value.toString());
            } catch (Exception e) {
                // Try parsing as SQL date
                String dateStr = value.toString();
                if (dateStr.length() == 10) { // YYYY-MM-DD format
                    try {
                        return LocalDate.parse(dateStr);
                    } catch (Exception ex) {
                        return null;
                    }
                }
            }
        }
        
        return null;
    }

    private BigDecimal extractBigDecimal(Struct struct, String fieldName) {
        if (struct == null) return null;
        Object value = struct.get(fieldName);
        
        if (value == null) return null;
        
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        
        if (value instanceof String) {
            try {
                return new BigDecimal(value.toString());
            } catch (Exception e) {
                return null;
            }
        }
        
        return null;
    }

    private boolean isNoticePeriodActive(LocalDate noticeStart, LocalDate noticeEnd, LocalDate currentDate) {
        if (noticeStart == null || noticeEnd == null) {
            return false;
        }
        
        return !currentDate.isBefore(noticeStart) && !currentDate.isAfter(noticeEnd);
    }

    private double calculateYearsBetween(LocalDate startDate, LocalDate endDate) {
        return ChronoUnit.DAYS.between(startDate, endDate) / 365.25;
    }
}
