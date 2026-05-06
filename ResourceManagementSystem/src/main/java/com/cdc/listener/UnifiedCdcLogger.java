package com.cdc.listener;

import io.debezium.engine.RecordChangeEvent;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.data.Struct;
import org.springframework.stereotype.Component;

/**
 * UNIFIED CDC Logger for BOTH PMS and EOS.
 * 
 * ELIMINATES code duplication by providing a generic logger that can handle
 * both PMS and EOS CDC events with consistent logging patterns.
 * 
 * Architecture:
 * - Generic logging implementation
 * - Configurable source identification
 * - Shared logging patterns
 * - Single source of truth for CDC event logging
 */
@Component
public class UnifiedCdcLogger {

    /**
     * Log CDC event with source identification.
     * 
     * @param event The CDC event
     * @param sourceType The source type ("PMS" or "EOS")
     */
    public void logEvent(RecordChangeEvent<SourceRecord> event, String sourceType) {
        SourceRecord record = event.record();
        Struct value = (Struct) record.value();

        if (value == null) {
            System.out.println(sourceType + " CDC Event: NULL value - skipping");
            return;
        }

        String op = value.getString("op"); // c, u, d, r
        Struct source = value.getStruct("source");
        
        String tableName = source != null ? source.getString("table") : "unknown";
        String operation = getOperationDescription(op);

        System.out.println(String.format(
            "%s CDC Event: %s on table %s at %s",
            sourceType,
            operation,
            tableName,
            java.time.LocalDateTime.now()
        ));

        // Log additional details if needed
        if (source != null) {
            String database = source.getString("db");
            String connector = source.getString("connector");
            System.out.println(String.format(
                "%s CDC Details: Database=%s, Connector=%s, Operation=%s",
                sourceType,
                database != null ? database : "unknown",
                connector != null ? connector : "unknown",
                op
            ));
        }
    }

    /**
     * Get human-readable operation description.
     */
    private String getOperationDescription(String op) {
        if (op == null) return "UNKNOWN";
        
        return switch (op) {
            case "c" -> "CREATE";
            case "u" -> "UPDATE";
            case "d" -> "DELETE";
            case "r" -> "SNAPSHOT_READ";
            default -> "UNKNOWN(" + op + ")";
        };
    }

    /**
     * Log error during CDC processing.
     * 
     * @param sourceType The source type ("PMS" or "EOS")
     * @param entityId The entity ID
     * @param operation The operation being performed
     * @param error The error that occurred
     */
    public void logError(String sourceType, String entityId, String operation, Exception error) {
        System.err.println(String.format(
            "%s CDC Error: Entity=%s, Operation=%s, Error=%s, Message=%s",
            sourceType,
            entityId,
            operation,
            error.getClass().getSimpleName(),
            error.getMessage()
        ));
    }

    /**
     * Log successful CDC processing.
     * 
     * @param sourceType The source type ("PMS" or "EOS")
     * @param entityId The entity ID
     * @param operation The operation performed
     * @param details Additional processing details
     */
    public void logSuccess(String sourceType, String entityId, String operation, String details) {
        System.out.println(String.format(
            "%s CDC Success: Entity=%s, Operation=%s, Details=%s",
            sourceType,
            entityId,
            operation,
            details
        ));
    }
}
