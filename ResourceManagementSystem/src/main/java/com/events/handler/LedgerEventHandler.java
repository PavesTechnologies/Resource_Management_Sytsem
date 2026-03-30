package com.events.handler;

import com.entity.ledger_entities.LedgerEventLog;
import com.entity_enums.ledger_enums.EventStatus;
import com.events.ledger_events.*;
import com.repo.ledger_repo.LedgerEventLogRepository;
import com.service_imple.ledger_service_impl.AvailabilityCalculationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class LedgerEventHandler {

    private final LedgerEventLogRepository eventLogRepository;
    private final AvailabilityCalculationService availabilityCalculationService;
    private final DeadLetterQueueService deadLetterQueueService;

    @EventListener
    @Async("ledgerEventHandlerExecutor")
    @Transactional
    public void handleAllocationChangedEvent(AllocationChangedEvent event) {
        try {
            if (!processEventWithIdempotency(event)) {
                return;
            }

            LocalDate startDate = event.getCalculationStartDate();
            LocalDate endDate = event.getCalculationEndDate();

            if (startDate != null && endDate != null && !startDate.isAfter(endDate)) {
                availabilityCalculationService.recalculateForDateRange(event.getResourceId(), startDate, endDate);
                markEventAsCompleted(event.getEventId());
            } else {
                markEventAsFailed(event.getEventId(), "Invalid date range");
            }

        } catch (Exception e) {
            log.error("Failed to handle allocation changed event: {}", e.getMessage(), e);
            handleEventFailure(event, e);
        }
    }

    @EventListener
    @Async("ledgerEventHandlerExecutor")
    @Transactional
    public void handleRoleOffEvent(RoleOffEvent event) {
        try {
            if (!processEventWithIdempotency(event)) {
                return;
            }

            LocalDate startDate = event.getCalculationStartDate();
            LocalDate endDate = event.getCalculationEndDate();

            if (startDate != null && endDate != null && !startDate.isAfter(endDate)) {
                availabilityCalculationService.recalculateForDateRange(event.getResourceId(), startDate, endDate);
                markEventAsCompleted(event.getEventId());
            } else {
                markEventAsFailed(event.getEventId(), "Invalid date range");
            }

        } catch (Exception e) {
            log.error("Failed to handle role-off event: {}", e.getMessage(), e);
            handleEventFailure(event, e);
        }
    }

    @EventListener
    @Async("ledgerEventHandlerExecutor")
    @Transactional
    public void handleResourceCreatedEvent(ResourceCreatedEvent event) {
        try {
            if (!processEventWithIdempotency(event)) {
                return;
            }

            LocalDate startDate = event.getCalculationStartDate();
            LocalDate endDate = event.getCalculationEndDate();

            if (startDate != null && endDate != null && !startDate.isAfter(endDate)) {
                availabilityCalculationService.recalculateForDateRange(event.getResourceId(), startDate, endDate);
                markEventAsCompleted(event.getEventId());
            } else {
                markEventAsFailed(event.getEventId(), "Invalid date range");
            }

        } catch (Exception e) {
            log.error("Failed to handle resource created event: {}", e.getMessage(), e);
            handleEventFailure(event, e);
        }
    }

    private boolean processEventWithIdempotency(BaseLedgerEvent event) {
        try {
            String eventId = event.getEventId();
            String eventHash = generateEventHash(event);
            Long resourceId = event.getResourceId();
            String eventType = event.getEventType();

            Optional<LedgerEventLog> existingEvent = eventLogRepository.findByEventId(eventId);
            
            if (existingEvent.isPresent()) {
                LedgerEventLog eventLog = existingEvent.get();
                if (eventLog.getProcessedFlag()) {
                    return false;
                } else if (eventLog.getStatus() == EventStatus.PROCESSING) {
                    return false;
                } else if (eventLog.getStatus() == EventStatus.RETRY_EXHAUSTED) {
                    return false;
                }
            }

            if (eventLogRepository.existsByEventHash(eventHash) && !existingEvent.isPresent()) {
                return false;
            }

            LedgerEventLog eventLog = existingEvent.orElseGet(() -> LedgerEventLog.builder()
                    .eventId(eventId)
                    .resourceId(resourceId)
                    .eventType(eventType)
                    .eventHash(eventHash)
                    .processedFlag(false)
                    .retryCount(0)
                    .status(EventStatus.PENDING)
                    .build());

            eventLog.setProcessingStartedAt(LocalDateTime.now());
            eventLog.setStatus(EventStatus.PROCESSING);
            eventLogRepository.save(eventLog);

            return true;

        } catch (Exception e) {
            log.error("Idempotency check failed for event {}: {}", event.getEventId(), e.getMessage(), e);
            return false;
        }
    }

    private String generateEventHash(BaseLedgerEvent event) {
        if (event instanceof AllocationChangedEvent) {
            return ((AllocationChangedEvent) event).generateEventHash();
        } else if (event instanceof RoleOffEvent) {
            return ((RoleOffEvent) event).generateEventHash();
        } else if (event instanceof ResourceCreatedEvent) {
            return ((ResourceCreatedEvent) event).generateEventHash();
        }
        return event.getEventId() + ":" + event.getResourceId() + ":" + event.getEventTimestamp();
    }

    private void markEventAsCompleted(String eventId) {
        try {
            eventLogRepository.markEventAsCompleted(eventId, EventStatus.SUCCESS, 
                    LocalDateTime.now(), LocalDateTime.now());
        } catch (Exception e) {
            log.error("Failed to mark event {} as completed: {}", eventId, e.getMessage(), e);
        }
    }

    private void markEventAsFailed(String eventId, String errorMessage) {
        try {
            eventLogRepository.markEventAsFailed(eventId, EventStatus.FAILED, 
                    errorMessage, LocalDateTime.now());
        } catch (Exception e) {
            log.error("Failed to mark event {} as failed: {}", eventId, e.getMessage(), e);
        }
    }

    private void handleEventFailure(BaseLedgerEvent event, Exception exception) {
        try {
            String eventId = event.getEventId();
            String errorMessage = exception.getMessage();
            
            Optional<LedgerEventLog> eventLogOpt = eventLogRepository.findByEventId(eventId);
            if (eventLogOpt.isPresent()) {
                LedgerEventLog eventLog = eventLogOpt.get();
                eventLog.setRetryCount(eventLog.getRetryCount() + 1);
                
                if (eventLog.getRetryCount() >= 3) {
                    eventLog.setStatus(EventStatus.RETRY_EXHAUSTED);
                    eventLogRepository.save(eventLog);
                    
                    deadLetterQueueService.addToDeadLetterQueue(event, exception, eventLog.getRetryCount());
                } else {
                    eventLog.setStatus(EventStatus.FAILED);
                    eventLog.setErrorMessage(errorMessage);
                    eventLogRepository.save(eventLog);
                }
            } else {
                deadLetterQueueService.addToDeadLetterQueue(event, exception, 1);
            }
            
        } catch (Exception e) {
            log.error("Failed to handle event failure for event {}: {}", event.getEventId(), e.getMessage(), e);
        }
    }
}
