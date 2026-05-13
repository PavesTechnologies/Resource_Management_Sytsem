package com.cdc.protection;

import com.cdc.util.CdcUtcSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
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

    private final CdcUtcSupport cdcUtcSupport;

    public StaleEventProtectionService(CdcUtcSupport cdcUtcSupport) {
        this.cdcUtcSupport = cdcUtcSupport;
    }

    /**
     * Check if incoming EOS event is stale compared to existing RMS state.
     * 
     * @param existingChangedAt Current RMS entity changedAt timestamp
     * @param incomingChangedAt Incoming EOS event timestamp
     * @param entityId Entity identifier for logging
     * @return true if event is stale (should be rejected), false otherwise
     */
    public boolean isStaleEvent(LocalDateTime existingChangedAt, LocalDateTime incomingChangedAt, String entityId) {
        return isStaleEvent(existingChangedAt, incomingChangedAt != null ? incomingChangedAt.toInstant(java.time.ZoneOffset.UTC) : null, entityId);
    }

    public boolean isStaleEvent(LocalDateTime existingChangedAt, Instant incomingChangedAt, String entityId) {
        if (existingChangedAt == null) {
            return false;
        }
        
        if (incomingChangedAt == null) {
            log.warn("Incoming event has no timestamp for entity: {}, allowing processing", entityId);
            return false;
        }

        Instant existingInstant = existingChangedAt.toInstant(java.time.ZoneOffset.UTC);
        if (incomingChangedAt.isBefore(existingInstant)) {
            log.warn("STALE EVENT DETECTED - Entity: {}, Existing: {}, Incoming: {}", 
                    entityId, existingInstant, incomingChangedAt);
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
        Instant instant = extractEventInstant(payload);
        return instant != null ? cdcUtcSupport.utcDateTime(instant) : null;
    }

    public Instant extractEventInstant(Object payload) {
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
                    return cdcUtcSupport.extractInstant(value);
                }
            }
        }
        
        return null;
    }
}
