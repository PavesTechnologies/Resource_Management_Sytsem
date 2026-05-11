package com.cdc.config;

import java.util.HashSet;
import java.util.Set;

/**
 * EOS source column names that trigger CDC processing into RMS.
 *
 * Column names here must match the actual EOS database column names exactly,
 * not the RMS field names. Only changes to these columns cause a Resource sync.
 */
public final class EosTrackedColumns {

    private static final Set<String> TRACKED_COLUMNS = new HashSet<>();

    static {
        // eos_v1.employee_details
        TRACKED_COLUMNS.add("employee_id");
        TRACKED_COLUMNS.add("first_name");
        TRACKED_COLUMNS.add("middle_name");
        TRACKED_COLUMNS.add("last_name");
        TRACKED_COLUMNS.add("work_email");       // maps to Resource.email
        TRACKED_COLUMNS.add("location");         // maps to Resource.workingLocation
        TRACKED_COLUMNS.add("total_experience"); // maps to Resource.experiance
        TRACKED_COLUMNS.add("joining_date");     // maps to Resource.dateOfJoining
        TRACKED_COLUMNS.add("employment_type");
        TRACKED_COLUMNS.add("work_mode");
        TRACKED_COLUMNS.add("employment_status");

        // eos_v1.offer_letter_details
        TRACKED_COLUMNS.add("mail");             // join key to Resource
        TRACKED_COLUMNS.add("designation");
        TRACKED_COLUMNS.add("total_ctc");        // maps to Resource.annualCtc
        TRACKED_COLUMNS.add("currency");         // maps to Resource.currencyType

        // eos_v1.employee_exit
        TRACKED_COLUMNS.add("last_working_day"); // maps to Resource.dateOfExit
        TRACKED_COLUMNS.add("notice_start_date");
        TRACKED_COLUMNS.add("notice_end_date");
        TRACKED_COLUMNS.add("exit_type");
        TRACKED_COLUMNS.add("status");
    }

    public static boolean isTrackedColumn(String eosColumn) {
        return TRACKED_COLUMNS.contains(eosColumn);
    }

    public static boolean containsTrackedChanges(Set<String> changedColumns) {
        if (changedColumns == null || changedColumns.isEmpty()) return false;
        return changedColumns.stream().anyMatch(EosTrackedColumns::isTrackedColumn);
    }

    public static Set<String> getTrackedColumns() {
        return new HashSet<>(TRACKED_COLUMNS);
    }

    public static int getTrackedColumnCount() {
        return TRACKED_COLUMNS.size();
    }

    public static Set<String> filterTrackedColumns(Set<String> changedColumns) {
        if (changedColumns == null || changedColumns.isEmpty()) return new HashSet<>();
        Set<String> result = new HashSet<>();
        for (String column : changedColumns) {
            if (isTrackedColumn(column)) result.add(column);
        }
        return result;
    }

    private EosTrackedColumns() {}
}
