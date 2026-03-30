package com.events.publisher;

import com.events.ledger_events.AllocationChangedEvent;
import com.events.ledger_events.BaseLedgerEvent;
import com.events.ledger_events.ResourceCreatedEvent;
import com.events.ledger_events.RoleOffEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LedgerEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public void publishAllocationChangedEvent(AllocationChangedEvent event) {
        try {
            if (event.getEventId() == null) {
                event.setEventId(generateEventId());
            }
            if (event.getEventTimestamp() == null) {
                event.setEventTimestamp(LocalDateTime.now());
            }
            if (event.getEventType() == null) {
                event.setEventType("ALLOCATION_CHANGED");
            }
            if (event.getEventSource() == null) {
                event.setEventSource("ALLOCATION_SERVICE");
            }

            log.info("Publishing allocation changed event: eventId={}, resourceId={}, allocationId={}, changeType={}", 
                    event.getEventId(), event.getResourceId(), event.getAllocationId(), event.getChangeType());

            eventPublisher.publishEvent(event);
            
        } catch (Exception e) {
            log.error("Failed to publish allocation changed event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish allocation changed event", e);
        }
    }

    public void publishRoleOffEvent(RoleOffEvent event) {
        try {
            if (event.getEventId() == null) {
                event.setEventId(generateEventId());
            }
            if (event.getEventTimestamp() == null) {
                event.setEventTimestamp(LocalDateTime.now());
            }
            if (event.getEventType() == null) {
                event.setEventType("ROLE_OFF");
            }
            if (event.getEventSource() == null) {
                event.setEventSource("ROLE_OFF_SERVICE");
            }

            log.info("Publishing role-off event: eventId={}, resourceId={}, allocationId={}, roleOffDate={}", 
                    event.getEventId(), event.getResourceId(), event.getAllocationId(), event.getRoleOffDate());

            eventPublisher.publishEvent(event);
            
        } catch (Exception e) {
            log.error("Failed to publish role-off event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish role-off event", e);
        }
    }

    public void publishResourceCreatedEvent(ResourceCreatedEvent event) {
        try {
            if (event.getEventId() == null) {
                event.setEventId(generateEventId());
            }
            if (event.getEventTimestamp() == null) {
                event.setEventTimestamp(LocalDateTime.now());
            }
            if (event.getEventType() == null) {
                event.setEventType("RESOURCE_CREATED");
            }
            if (event.getEventSource() == null) {
                event.setEventSource("RESOURCE_SERVICE");
            }

            log.info("Publishing resource created event: eventId={}, resourceId={}, employeeId={}, dateOfJoining={}", 
                    event.getEventId(), event.getResourceId(), event.getEmployeeId(), event.getDateOfJoining());

            eventPublisher.publishEvent(event);
            
        } catch (Exception e) {
            log.error("Failed to publish resource created event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish resource created event", e);
        }
    }

    public void publishGenericLedgerEvent(BaseLedgerEvent event) {
        try {
            if (event.getEventId() == null) {
                event.setEventId(generateEventId());
            }
            if (event.getEventTimestamp() == null) {
                event.setEventTimestamp(LocalDateTime.now());
            }
            if (event.getEventSource() == null) {
                event.setEventSource("GENERIC_SERVICE");
            }

            log.info("Publishing generic ledger event: eventId={}, resourceId={}, eventType={}", 
                    event.getEventId(), event.getResourceId(), event.getEventType());

            eventPublisher.publishEvent(event);
            
        } catch (Exception e) {
            log.error("Failed to publish generic ledger event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish generic ledger event", e);
        }
    }

    private String generateEventId() {
        return "LEDGER-" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }

    public boolean isEventValid(BaseLedgerEvent event) {
        return event != null && 
               event.getEventId() != null && !event.getEventId().trim().isEmpty() &&
               event.getResourceId() != null &&
               event.getEventTimestamp() != null &&
               event.getEventType() != null && !event.getEventType().trim().isEmpty();
    }

    public void validateAndPublish(BaseLedgerEvent event) {
        if (!isEventValid(event)) {
            throw new IllegalArgumentException("Invalid event data: " + event);
        }
        
        if (event instanceof AllocationChangedEvent) {
            publishAllocationChangedEvent((AllocationChangedEvent) event);
        } else if (event instanceof RoleOffEvent) {
            publishRoleOffEvent((RoleOffEvent) event);
        } else if (event instanceof ResourceCreatedEvent) {
            publishResourceCreatedEvent((ResourceCreatedEvent) event);
        } else {
            publishGenericLedgerEvent(event);
        }
    }
}
