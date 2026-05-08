package com.cdc.protection;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * ENTERPRISE-GRADE Stale Event Overwrite Protection Service.
 * 
 * CRITICAL SAFETY MECHANISM to prevent older EOS replay/retry events 
 * from overwriting newer RMS state.
 * 
 * Reused by BOTH PMS and EOS CDC handlers for consistency.
 */
@Slf4j
@Service
public class StaleEventProtectionService {

    /**
     * Check if incoming EOS event is stale compared to existing RMS state.
     * 
     * @param existingChangedAt Current RMS entity changedAt timestamp
     * @param incomingChangedAt Incoming EOS event timestamp
     * @param entityId Entity identifier for logging
     * @return true if event is stale (should be rejected), false otherwise
     */
    public boolean isStaleEvent(LocalDateTime existingChangedAt, LocalDateTime incomingChangedAt, String entityId) {
        if (existingChangedAt == null) {
            // No existing timestamp - allow processing
            return false;
        }
        
        if (incomingChangedAt == null) {
            // No incoming timestamp - log warning but allow processing
            log.warn("Incoming event has no timestamp for entity: {}, allowing processing", entityId);
            return false;
        }
        
        // CRITICAL: Reject if incoming event is older than existing state
        if (incomingChangedAt.isBefore(existingChangedAt)) {
            log.warn("STALE EVENT DETECTED - Entity: {}, Existing: {}, Incoming: {}", 
                    entityId, existingChangedAt, incomingChangedAt);
            return true;
        }
        
        return false;
    }

    /**
     * Extract timestamp from EOS Debezium payload.
     * 
     * @param payload Debezium after/before struct
     * @return Extracted timestamp or null if not found
     */
    public LocalDateTime extractEventTimestamp(Object payload) {
        if (payload == null) {
            return null;
        }
        
        // Handle different timestamp field names from EOS
        if (payload instanceof org.apache.kafka.connect.data.Struct) {
            org.apache.kafka.connect.data.Struct struct = (org.apache.kafka.connect.data.Struct) payload;
            
            // Try common timestamp fields
            String[] timestampFields = {"updated_at", "modified_at", "last_updated", "change_timestamp", "event_time"};
            
            for (String field : timestampFields) {
                Object value = struct.get(field);
                if (value != null) {
                    return convertToTimestamp(value, field);
                }
            }
        }
        
        return null;
    }

    /**
     * Convert various timestamp formats to LocalDateTime.
     * Reuses existing CDC timestamp conversion logic.
     */
    private LocalDateTime convertToTimestamp(Object value, String fieldName) {
        if (value == null) return null;
        
        // Handle LocalDateTime directly
        if (value instanceof LocalDateTime) return (LocalDateTime) value;
        
        // Handle java.sql.Timestamp
        if (value instanceof java.sql.Timestamp) {
            return ((java.sql.Timestamp) value).toLocalDateTime();
        }
        
        // Handle java.time.Instant
        if (value instanceof java.time.Instant) {
            return LocalDateTime.ofInstant((java.time.Instant) value, java.time.ZoneId.systemDefault());
        }
        
        // Handle java.util.Date
        if (value instanceof java.util.Date) {
            return new java.sql.Timestamp(((java.util.Date) value).getTime()).toLocalDateTime();
        }
        
        // Handle Long (timestamp)
        if (value instanceof Long) {
            return convertTimestampToDateTime((Long) value, fieldName);
        }
        
        // Handle String conversion
        if (value instanceof String) {
            return convertStringToDateTime((String) value, fieldName);
        }
        
        log.warn("Unsupported timestamp type for field {}: {}", fieldName, value.getClass().getName());
        return null;
    }
    
    private LocalDateTime convertTimestampToDateTime(long timestamp, String fieldName) {
        try {
            if (timestamp > 1_000_000_000_000_000L) { // microseconds (16+ digits)
                return LocalDateTime.ofInstant(java.time.Instant.ofEpochSecond(timestamp / 1_000_000, (timestamp % 1_000_000) * 1000), java.time.ZoneId.systemDefault());
            } else if (timestamp > 1_000_000_000_000L) { // milliseconds (13 digits)
                return LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(timestamp), java.time.ZoneId.systemDefault());
            } else { // seconds (10 digits)
                return LocalDateTime.ofInstant(java.time.Instant.ofEpochSecond(timestamp), java.time.ZoneId.systemDefault());
            }
        } catch (Exception e) {
            log.warn("Failed to convert Long timestamp for field {}: {}", fieldName, e.getMessage());
            return null;
        }
    }
    
    private LocalDateTime convertStringToDateTime(String strValue, String fieldName) {
        try {
            strValue = strValue.trim();
            if (strValue.isEmpty()) return null;
            
            // Try common date formats
            if (strValue.contains("T")) {
                return LocalDateTime.parse(strValue);
            } else {
                // Try parsing as date
                java.time.LocalDate date = java.time.LocalDate.parse(strValue);
                return date.atStartOfDay();
            }
        } catch (Exception e) {
            log.warn("Failed to parse string date for field {}: {}", fieldName, e.getMessage());
            return null;
        }
    }
}
