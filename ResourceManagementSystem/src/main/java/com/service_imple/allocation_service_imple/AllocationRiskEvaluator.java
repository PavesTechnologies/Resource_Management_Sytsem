package com.service_imple.allocation_service_imple;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Utility class for risk evaluation in skill gap matching
 * Handles recency risk and overall risk aggregation
 */
public class AllocationRiskEvaluator {
    
    // Risk level constants
    public static final String ALLOCATION_ALLOCATION_RISK_LOW = "LOW";
    public static final String ALLOCATION_ALLOCATION_RISK_MEDIUM = "MEDIUM";
    public static final String ALLOCATION_ALLOCATION_RISK_HIGH = "HIGH";
    
    /**
     * Evaluates recency risk based on lastUsedDate
     * 
     * @param lastUsedDate Date when skill was last used
     * @return Risk level (LOW, MEDIUM, HIGH)
     */
    public static String evaluateRecencyRisk(LocalDate lastUsedDate) {
        if (lastUsedDate == null) {
            return ALLOCATION_ALLOCATION_RISK_HIGH;
        }
        
        long yearsUnused = ChronoUnit.YEARS.between(lastUsedDate, LocalDate.now());
        
        if (yearsUnused > 5) {
            return ALLOCATION_ALLOCATION_RISK_HIGH;
        } else if (yearsUnused > 3) {
            return ALLOCATION_ALLOCATION_RISK_MEDIUM;
        } else if (yearsUnused > 2) {
            return ALLOCATION_ALLOCATION_RISK_LOW;
        } else {
            return ALLOCATION_ALLOCATION_RISK_LOW; // ≤ 2 years
        }
    }
    
    /**
     * Calculates years unused for recency warning
     * 
     * @param lastUsedDate Date when skill was last used
     * @return Number of years unused
     */
    public static long calculateYearsUnused(LocalDate lastUsedDate) {
        if (lastUsedDate == null) {
            return 999L; // Indicate very old/unknown
        }
        return ChronoUnit.YEARS.between(lastUsedDate, LocalDate.now());
    }
    
    /**
     * Aggregates overall risk level from multiple risk factors
     * 
     * @param mandatoryGapRisk Risk from mandatory gaps
     * @param partialRisk Risk from partial matches
     * @param recencyRisk Risk from skill recency
     * @return Final aggregated risk level
     */
    public static String aggregateRisk(String mandatoryGapRisk, String partialRisk, String recencyRisk) {
        // Priority order: HIGH > MEDIUM > LOW
        if (ALLOCATION_ALLOCATION_RISK_HIGH.equals(mandatoryGapRisk) || ALLOCATION_ALLOCATION_RISK_HIGH.equals(partialRisk) || ALLOCATION_ALLOCATION_RISK_HIGH.equals(recencyRisk)) {
            return ALLOCATION_ALLOCATION_RISK_HIGH;
        } else if (ALLOCATION_ALLOCATION_RISK_MEDIUM.equals(mandatoryGapRisk) || ALLOCATION_ALLOCATION_RISK_MEDIUM.equals(partialRisk) || ALLOCATION_ALLOCATION_RISK_MEDIUM.equals(recencyRisk)) {
            return ALLOCATION_ALLOCATION_RISK_MEDIUM;
        } else {
            return ALLOCATION_ALLOCATION_RISK_LOW;
        }
    }
    
    /**
     * Determines allocation eligibility based on risk factors
     * 
     * @param hasMandatoryGap Whether there are mandatory skill gaps
     * @param hasMandatoryPartial Whether there are mandatory partial matches
     * @return true if allocation is allowed, false otherwise
     */
    public static boolean isAllocationAllowed(boolean hasMandatoryGap, boolean hasMandatoryPartial) {
        // Mandatory GAP blocks allocation
        // Mandatory PARTIAL allows allocation (HIGH risk)
        return !hasMandatoryGap;
    }
}
