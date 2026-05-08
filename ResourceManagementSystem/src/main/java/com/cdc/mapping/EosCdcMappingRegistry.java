package com.cdc.mapping;

import java.util.HashMap;
import java.util.Map;

/**
 * DIRECT EOS → RMS Resource field mapping registry.
 * 
 * This registry contains ONLY DIRECT field mappings from EOS to RMS.
 * Derived fields are handled separately by ResourceDerivationService.
 * 
 * Architecture:
 * PHASE 1: Direct field mapping (handled by this registry)
 * PHASE 2: Derived field calculation (handled by ResourceDerivationService)
 * 
 * Design Principles:
 * - Only direct EOS source → RMS target mappings
 * - No derived field logic or business calculations
 * - String-based resourceId architecture
 * - Production-safe and CDC-friendly
 */
public final class EosCdcMappingRegistry {

    private static final Map<String, ColumnMapping> EOS_TO_RESOURCE = new HashMap<>();

    static {
        // DIRECT FIELD MAPPINGS ONLY
        // These are exact EOS source → RMS target mappings
        // Derived fields are calculated separately by ResourceDerivationService

        // Core Resource Identity Mappings
        EOS_TO_RESOURCE.put("employee_id",
                new ColumnMapping("employee_id", "resourceId", FieldType.STRING, null));

        EOS_TO_RESOURCE.put("full_name",
                new ColumnMapping("full_name", "fullName", FieldType.STRING, null));

        EOS_TO_RESOURCE.put("email",
                new ColumnMapping("email", "email", FieldType.STRING, null));

        // Professional Information Mappings
        EOS_TO_RESOURCE.put("designation",
                new ColumnMapping("designation", "designation", FieldType.STRING, null));

        EOS_TO_RESOURCE.put("working_location",
                new ColumnMapping("working_location", "workingLocation", FieldType.STRING, null));

        // Date-related Mappings (Source data for derived calculations)
        EOS_TO_RESOURCE.put("date_of_joining",
                new ColumnMapping("date_of_joining", "dateOfJoining", FieldType.LOCAL_DATE, null));

        EOS_TO_RESOURCE.put("date_of_exit",
                new ColumnMapping("date_of_exit", "dateOfExit", FieldType.LOCAL_DATE, null));

        EOS_TO_RESOURCE.put("notice_start_date",
                new ColumnMapping("notice_start_date", "noticeStartDate", FieldType.LOCAL_DATE, null));

        EOS_TO_RESOURCE.put("notice_end_date",
                new ColumnMapping("notice_end_date", "noticeEndDate", FieldType.LOCAL_DATE, null));

        // Financial Information Mappings
        EOS_TO_RESOURCE.put("annual_ctc",
                new ColumnMapping("annual_ctc", "annualCtc", FieldType.BIG_DECIMAL, null));

        EOS_TO_RESOURCE.put("currency_type",
                new ColumnMapping("currency_type", "currencyType", FieldType.STRING, null));

        EOS_TO_RESOURCE.put("hourly_cost_rate",
                new ColumnMapping("hourly_cost_rate", "hourlyCostRate", FieldType.BIG_DECIMAL, null));

        // Audit Trail Mappings
        EOS_TO_RESOURCE.put("changed_by",
                new ColumnMapping("changed_by", "changedBy", FieldType.STRING, null));

        EOS_TO_RESOURCE.put("updated_at",
                new ColumnMapping("updated_at", "changedAt", FieldType.LOCAL_DATE_TIME, null));

        EOS_TO_RESOURCE.put("created_at",
                new ColumnMapping("created_at", "createdAt", FieldType.LOCAL_DATE_TIME, null));
    }

    private EosCdcMappingRegistry() {}

    /**
     * Get mapping for a specific EOS column.
     * Returns null if no mapping exists.
     */
    public static ColumnMapping getMapping(String eosColumn) {
        return EOS_TO_RESOURCE.get(eosColumn);
    }

    /**
     * Check if a mapping exists for the given EOS column.
     */
    public static boolean hasMapping(String eosColumn) {
        return EOS_TO_RESOURCE.containsKey(eosColumn);
    }

    /**
     * Get all mappings.
     * Returns an immutable copy of the mappings.
     */
    public static Map<String, ColumnMapping> getAllMappings() {
        return new HashMap<>(EOS_TO_RESOURCE);
    }
}
