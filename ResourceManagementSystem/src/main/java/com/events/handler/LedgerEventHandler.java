package com.events.handler;

import com.cdc.listener.EosCdcHandler;
import com.cdc.listener.PmsCdcHandler;
import com.cdc.payload.CdcEventPayload;
import com.cdc.payload.CdcPayloadCodec;
import com.cdc.util.CdcUtcSupport;
import com.entity.ledger_entities.LedgerEventLog;
import com.entity_enums.ledger_enums.EventStatus;
import com.events.ledger_events.AllocationChangedEvent;
import com.events.ledger_events.BaseLedgerEvent;
import com.events.ledger_events.ResourceCreatedEvent;
import com.events.ledger_events.RoleOffLedgerEvent;
import com.repo.ledger_repo.LedgerEventLogRepository;
import com.service_interface.ledger_service_interface.LedgerAvailabilityCalculationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class LedgerEventHandler {

    private final LedgerEventLogRepository eventLogRepository;
    private final LedgerAvailabilityCalculationService availabilityCalculationService;
    private final DeadLetterQueueService deadLetterQueueService;
    private final PmsCdcHandler pmsCdcHandler;
    private final EosCdcHandler eosCdcHandler;
    private final CdcPayloadCodec cdcPayloadCodec;
    private final CdcUtcSupport cdcUtcSupport;
    private final String processorOwner = System.getenv().getOrDefault("HOSTNAME", UUID.randomUUID().toString());
    private static final int MAX_RETRY_COUNT = 5;

    @EventListener
    @Async("ledgerEventHandlerExecutor")
    @Transactional
    public void handleAllocationChangedEvent(AllocationChangedEvent event) {
        handleLedgerEvent(event);
    }

    @EventListener
    @Async("ledgerEventHandlerExecutor")
    @Transactional
    public void handleRoleOffEvent(RoleOffLedgerEvent event) {
        handleLedgerEvent(event);
    }

    @EventListener
    @Async("ledgerEventHandlerExecutor")
    @Transactional
    public void handleResourceCreatedEvent(ResourceCreatedEvent event) {
        handleLedgerEvent(event);
    }

    @Transactional
    public int processPendingCdcEvents(int batchSize) {
        List<LedgerEventLog> candidates = eventLogRepository.findReadyCdcEvents(
                List.of(EventStatus.NEW, EventStatus.RETRY_SCHEDULED, EventStatus.PENDING, EventStatus.FAILED),
                cdcUtcSupport.utcDateTime(cdcUtcSupport.now()),
                PageRequest.of(0, batchSize)
        );

        int processed = 0;
        for (LedgerEventLog candidate : candidates) {
            if (claimAndProcess(candidate)) {
                processed++;
            }
        }
        return processed;
    }

    @Transactional
    public int recoverStalledCdcEvents() {
        List<LedgerEventLog> stalled = eventLogRepository.findStalledCdcEvents(
                EventStatus.PROCESSING,
                cdcUtcSupport.utcDateTime(cdcUtcSupport.now().minusSeconds(120))
        );
        int recovered = 0;
        for (LedgerEventLog event : stalled) {
            event.setStatus(EventStatus.RETRY_SCHEDULED);
            event.setNextRetryAt(cdcUtcSupport.utcDateTime(cdcUtcSupport.now().plusSeconds(30)));
            event.setClaimOwner(null);
            event.setErrorMessage("Recovered stalled CDC processing");
            eventLogRepository.save(event);
            recovered++;
        }
        return recovered;
    }

    private boolean claimAndProcess(LedgerEventLog candidate) {
        Instant claimedAt = cdcUtcSupport.now();
        LocalDateTime updatedAt = cdcUtcSupport.utcDateTime(claimedAt);
        int claimed = eventLogRepository.claimEvent(
                candidate.getId(),
                List.of(EventStatus.NEW, EventStatus.RETRY_SCHEDULED, EventStatus.PENDING, EventStatus.FAILED),
                EventStatus.CLAIMED,
                processorOwner,
                claimedAt,
                updatedAt
        );
        if (claimed == 0) {
            return false;
        }

        eventLogRepository.markClaimedEventAsProcessing(
                candidate.getId(),
                EventStatus.CLAIMED,
                EventStatus.PROCESSING,
                processorOwner,
                updatedAt,
                updatedAt
        );

        LedgerEventLog eventLog = eventLogRepository.findById(candidate.getId()).orElse(null);
        if (eventLog == null) {
            return false;
        }

        try {
            processCdcEvent(eventLog);
            eventLogRepository.markCdcEventAsSuccess(
                    eventLog.getId(),
                    processorOwner,
                    EventStatus.SUCCESS,
                    cdcUtcSupport.utcDateTime(cdcUtcSupport.now()),
                    cdcUtcSupport.utcDateTime(cdcUtcSupport.now())
            );
            return true;
        } catch (Exception ex) {
            handleCdcFailure(eventLog, ex);
            return false;
        }
    }

    private void processCdcEvent(LedgerEventLog eventLog) {
        CdcEventPayload payload = cdcPayloadCodec.deserialize(eventLog.getPayload());
        if ("PMS".equalsIgnoreCase(eventLog.getConnectorName())) {
            pmsCdcHandler.processInboxEvent(payload);
        } else if ("EOS".equalsIgnoreCase(eventLog.getConnectorName())) {
            eosCdcHandler.processInboxEvent(payload);
        } else {
            throw new IllegalStateException("Unsupported CDC connector: " + eventLog.getConnectorName());
        }
    }

    private void handleCdcFailure(LedgerEventLog eventLog, Exception exception) {
        int nextRetryCount = (eventLog.getRetryCount() == null ? 0 : eventLog.getRetryCount()) + 1;
        if (nextRetryCount >= MAX_RETRY_COUNT) {
            eventLogRepository.markCdcEventAsDeadLetter(
                    eventLog.getId(),
                    processorOwner,
                    EventStatus.DEAD_LETTER,
                    exception.getMessage(),
                    cdcUtcSupport.now(),
                    cdcUtcSupport.utcDateTime(cdcUtcSupport.now())
            );
            deadLetterQueueService.addCdcEventToDeadLetterQueue(eventLog, exception, nextRetryCount);
            return;
        }

        LocalDateTime nextRetryAt = cdcUtcSupport.utcDateTime(cdcUtcSupport.now().plusSeconds((long) Math.pow(2, nextRetryCount) * 30L));
        eventLogRepository.rescheduleCdcEvent(
                eventLog.getId(),
                processorOwner,
                EventStatus.RETRY_SCHEDULED,
                exception.getMessage(),
                nextRetryCount,
                nextRetryAt,
                cdcUtcSupport.now(),
                cdcUtcSupport.utcDateTime(cdcUtcSupport.now())
        );
    }

    private void handleLedgerEvent(BaseLedgerEvent event) {
        try {
            if (!processEventWithIdempotency(event)) {
                return;
            }

            LocalDate startDate = null;
            LocalDate endDate = null;
            if (event instanceof AllocationChangedEvent allocationChangedEvent) {
                startDate = allocationChangedEvent.getCalculationStartDate();
                endDate = allocationChangedEvent.getCalculationEndDate();
            } else if (event instanceof RoleOffLedgerEvent roleOffLedgerEvent) {
                startDate = roleOffLedgerEvent.getCalculationStartDate();
                endDate = roleOffLedgerEvent.getCalculationEndDate();
            } else if (event instanceof ResourceCreatedEvent resourceCreatedEvent) {
                startDate = resourceCreatedEvent.getCalculationStartDate();
                endDate = resourceCreatedEvent.getCalculationEndDate();
            }
            if (startDate != null && endDate != null && !startDate.isAfter(endDate)) {
                availabilityCalculationService.recalculateForDateRange(event.getResourceId(), startDate, endDate);
                markEventAsCompleted(event.getEventId());
            } else {
                markEventAsFailed(event.getEventId(), "Invalid date range");
            }
        } catch (Exception e) {
            log.error("Failed to handle ledger event {}: {}", event.getEventId(), e.getMessage(), e);
            handleLedgerEventFailure(event, e);
        }
    }

    private boolean processEventWithIdempotency(BaseLedgerEvent event) {
        try {
            String eventId = event.getEventId();
            String eventHash = generateEventHash(event);
            String resourceId = event.getResourceId();
            String eventType = event.getEventType();

            Optional<LedgerEventLog> existingEvent = eventLogRepository.findByEventId(eventId);
            if (existingEvent.isPresent()) {
                LedgerEventLog eventLog = existingEvent.get();
                if (Boolean.TRUE.equals(eventLog.getProcessedFlag())
                        || eventLog.getStatus() == EventStatus.PROCESSING
                        || eventLog.getStatus() == EventStatus.DEAD_LETTER
                        || eventLog.getStatus() == EventStatus.RETRY_EXHAUSTED
                        || eventLog.getStatus() == EventStatus.PERMANENTLY_FAILED) {
                    return false;
                }
            }

            if (eventLogRepository.existsByEventHash(eventHash) && existingEvent.isEmpty()) {
                return false;
            }

            LedgerEventLog eventLog = existingEvent.orElseGet(() -> LedgerEventLog.builder()
                    .eventId(eventId)
                    .resourceId(resourceId)
                    .eventType(eventType)
                    .eventHash(eventHash)
                    .processedFlag(false)
                    .retryCount(0)
                    .status(EventStatus.NEW)
                    .build());

            eventLog.setProcessingStartedAt(cdcUtcSupport.utcDateTime(cdcUtcSupport.now()));
            eventLog.setStatus(EventStatus.PROCESSING);
            eventLogRepository.save(eventLog);
            return true;
        } catch (Exception e) {
            log.error("Idempotency check failed for event {}: {}", event.getEventId(), e.getMessage(), e);
            return false;
        }
    }

    private String generateEventHash(BaseLedgerEvent event) {
        return event.generateEventHash();
    }

    private void markEventAsCompleted(String eventId) {
        eventLogRepository.markEventAsCompleted(
                eventId,
                EventStatus.SUCCESS,
                cdcUtcSupport.utcDateTime(cdcUtcSupport.now()),
                cdcUtcSupport.utcDateTime(cdcUtcSupport.now())
        );
    }

    private void markEventAsFailed(String eventId, String errorMessage) {
        eventLogRepository.markEventAsFailed(
                eventId,
                EventStatus.RETRY_SCHEDULED,
                errorMessage,
                cdcUtcSupport.utcDateTime(cdcUtcSupport.now())
        );
    }

    private void handleLedgerEventFailure(BaseLedgerEvent event, Exception exception) {
        Optional<LedgerEventLog> eventLogOpt = eventLogRepository.findByEventId(event.getEventId());
        if (eventLogOpt.isEmpty()) {
            deadLetterQueueService.addToDeadLetterQueue(event, exception, 1);
            return;
        }

        LedgerEventLog eventLog = eventLogOpt.get();
        eventLog.setRetryCount((eventLog.getRetryCount() == null ? 0 : eventLog.getRetryCount()) + 1);
        if (eventLog.getRetryCount() >= 3) {
            eventLog.setStatus(EventStatus.DEAD_LETTER);
            eventLogRepository.save(eventLog);
            deadLetterQueueService.addToDeadLetterQueue(event, exception, eventLog.getRetryCount());
        } else {
            eventLog.setStatus(EventStatus.RETRY_SCHEDULED);
            eventLog.setErrorMessage(exception.getMessage());
            eventLog.setNextRetryAt(cdcUtcSupport.utcDateTime(cdcUtcSupport.now().plus(Duration.ofMinutes(15))));
            eventLogRepository.save(eventLog);
        }
    }
}
