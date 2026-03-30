package com.events.ledger_events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseLedgerEvent {

    protected String eventId;
    protected Long resourceId;
    protected LocalDateTime eventTimestamp;
    protected String eventType;
    protected String eventSource;

    public abstract String generateEventHash();
    
    public abstract String getEventId();
    
    public abstract Long getResourceId();
    
    public abstract LocalDateTime getEventTimestamp();
    
    public abstract String getEventType();

    public boolean isValid() {
        return eventId != null && !eventId.trim().isEmpty() &&
               resourceId != null &&
               eventTimestamp != null &&
               eventType != null && !eventType.trim().isEmpty();
    }

    public String getUniqueKey() {
        return eventId + ":" + resourceId + ":" + eventType;
    }
}
