package com.cdc.config;

import java.util.Set;
import java.util.HashSet;

/**
 * Configuration for tracked EOS columns that trigger CDC processing.
 * 
 * Only changes to these columns will trigger EOS → RMS synchronization.
 * This reduces unnecessary processing and improves CDC performance.
 * 
 * Tracked columns are the core business fields that affect RMS Resource state.
 * Audit and metadata columns are excluded to reduce CDC noise.
 */
public final class EosTrackedColumns {

    /**
     * Set of EOS columns that trigger CDC processing.
     * Changes to these columns indicate meaningful business changes.
     */
    private static final Set<String> TRACKED_COLUMNS = new HashSet<>();

    static {
        // CDC TRIGGER COLUMNS - Only these changes trigger Resource sync
        
        // Core Identity Columns
        TRACKED_COLUMNS.add("employee_id");
        TRACKED_COLUMNS.add("full_name");
        TRACKED_COLUMNS.add("email");

        // Professional Information
        TRACKED_COLUMNS.add("designation");
        TRACKED_COLUMNS.add("working_location");

        // Employment Status Columns (Source data for derivations)
        TRACKED_COLUMNS.add("active_flag");

        // Date-related Columns (Critical for status calculations)
        TRACKED_COLUMNS.add("date_of_joining");
        TRACKED_COLUMNS.add("date_of_exit");
        TRACKED_COLUMNS.add("notice_start_date");
        TRACKED_COLUMNS.add("notice_end_date");

        // Financial Information
        TRACKED_COLUMNS.add("annual_ctc");
        TRACKED_COLUMNS.add("currency_type");
        TRACKED_COLUMNS.add("hourly_cost_rate");
    }

    /**
     * Check if a column is tracked for CDC processing.
     * 
     * @param eosColumn The EOS column name
     * @return true if the column is tracked, false otherwise
     */
    public static boolean isTrackedColumn(String eosColumn) {
        return TRACKED_COLUMNS.contains(eosColumn);
    }

    /**
     * Check if any of the changed columns are tracked.
     * 
     * @param changedColumns Set of changed column names
     * @return true if any tracked columns changed, false otherwise
     */
    public static boolean containsTrackedChanges(Set<String> changedColumns) {
        if (changedColumns == null || changedColumns.isEmpty()) {
            return false;
        }

        return changedColumns.stream()
                .anyMatch(EosTrackedColumns::isTrackedColumn);
    }

    /**
     * Get all tracked columns.
     * 
     * @return Immutable set of tracked column names
     */
    public static Set<String> getTrackedColumns() {
        return new HashSet<>(TRACKED_COLUMNS);
    }

    /**
     * Get the count of tracked columns.
     * 
     * @return Number of tracked columns
     */
    public static int getTrackedColumnCount() {
        return TRACKED_COLUMNS.size();
    }

    /**
     * Filter changed columns to only include tracked ones.
     * 
     * @param changedColumns Set of all changed columns
     * @return Set containing only tracked changed columns
     */
    public static Set<String> filterTrackedColumns(Set<String> changedColumns) {
        if (changedColumns == null || changedColumns.isEmpty()) {
            return new HashSet<>();
        }

        Set<String> trackedChanges = new HashSet<>();
        for (String column : changedColumns) {
            if (isTrackedColumn(column)) {
                trackedChanges.add(column);
            }
        }
        return trackedChanges;
    }

    private EosTrackedColumns() {
        // Utility class - prevent instantiation
    }
}
