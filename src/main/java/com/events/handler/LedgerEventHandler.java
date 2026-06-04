package com.events.handler;

import com.cdc.config.properties.OfferLifecycleProperties;
import com.cdc.model.CdcProcessingOutcome;
import com.cdc.service.InboxEventProcessor;
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
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
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
    private final CdcUtcSupport cdcUtcSupport;
    private final InboxEventProcessor inboxEventProcessor;
    private final ObjectProvider<LedgerEventHandler> ledgerEventHandlerProvider;
    private final OfferLifecycleProperties offerLifecycleProperties;
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

    public int processPendingCdcEvents(int batchSize) {
        List<LedgerEventLog> candidates = eventLogRepository.findReadyCdcEvents(
                List.of(EventStatus.NEW, EventStatus.RETRY_SCHEDULED, EventStatus.WAITING_FOR_DEPENDENCY, EventStatus.PENDING, EventStatus.FAILED),
                cdcUtcSupport.utcDateTime(cdcUtcSupport.now()),
                PageRequest.of(0, batchSize)
        );

        log.info("CDC polling cycle started: owner={}, batchSize={}, candidates={}",
                processorOwner, batchSize, candidates.size());

        int processed = 0;
        for (LedgerEventLog candidate : candidates) {
            try {
                if (ledgerEventHandlerProvider.getObject().claimAndProcess(candidate)) {
                    processed++;
                }
            } catch (Exception ex) {
                log.error("CDC per-event transaction failed before completion for eventId={}, owner={}, cause={}",
                        candidate.getEventId(), processorOwner, ex.getMessage(), ex);
            }
        }

        log.info("CDC polling cycle completed: owner={}, processed={}, candidates={}",
                processorOwner, processed, candidates.size());

        return processed;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean claimAndProcess(LedgerEventLog candidate) {
        Instant claimedAt = cdcUtcSupport.now();
        LocalDateTime updatedAt = cdcUtcSupport.utcDateTime(claimedAt);

        log.info("Attempting CDC claim: eventId={}, ledgerId={}, owner={}, currentStatus={}",
                candidate.getEventId(), candidate.getId(), processorOwner, candidate.getStatus());

        int claimed = eventLogRepository.claimEvent(
                candidate.getId(),
                List.of(EventStatus.NEW, EventStatus.RETRY_SCHEDULED, EventStatus.WAITING_FOR_DEPENDENCY, EventStatus.PENDING, EventStatus.FAILED),
                EventStatus.CLAIMED,
                processorOwner,
                claimedAt,
                updatedAt
        );

        if (claimed == 0) {
            log.debug("CDC claim skipped because another node already handled it: eventId={}, ledgerId={}, owner={}",
                    candidate.getEventId(), candidate.getId(), processorOwner);
            return false;
        }

        log.info("CDC claim acquired: eventId={}, ledgerId={}, owner={}, claimedAt={}",
                candidate.getEventId(), candidate.getId(), processorOwner, claimedAt);

        int markedProcessing = eventLogRepository.markClaimedEventAsProcessing(
                candidate.getId(),
                EventStatus.CLAIMED,
                EventStatus.PROCESSING,
                processorOwner,
                updatedAt,
                updatedAt
        );

        if (markedProcessing == 0) {
            log.warn("CDC claim ownership lost before processing state transition: eventId={}, ledgerId={}, owner={}",
                    candidate.getEventId(), candidate.getId(), processorOwner);
            return false;
        }

        LedgerEventLog eventLog = eventLogRepository.findById(candidate.getId()).orElse(null);
        if (eventLog == null) {
            log.warn("CDC claimed event disappeared before processing: ledgerId={}, owner={}",
                    candidate.getId(), processorOwner);
            return false;
        }

        try {
            CdcProcessingOutcome processingOutcome = inboxEventProcessor.processSingleEvent(eventLog);

            if (processingOutcome.getOutcomeType() == CdcProcessingOutcome.OutcomeType.WAITING_FOR_DEPENDENCY) {
                handleWaitingOutcome(eventLog, processingOutcome);
                return true;
            }

            if (processingOutcome.getOutcomeType() == CdcProcessingOutcome.OutcomeType.CANCELLED) {
                handleCancelledOutcome(eventLog, processingOutcome);
                return true;
            }

            int successUpdated = eventLogRepository.markCdcEventAsSuccess(
                    eventLog.getId(),
                    processorOwner,
                    EventStatus.SUCCESS,
                    cdcUtcSupport.utcDateTime(cdcUtcSupport.now()),
                    cdcUtcSupport.utcDateTime(cdcUtcSupport.now())
            );

            if (successUpdated == 0) {
                throw new IllegalStateException("CDC success transition rejected for owner " + processorOwner);
            }

            log.info("CDC event completed successfully: eventId={}, ledgerId={}, owner={}",
                    eventLog.getEventId(), eventLog.getId(), processorOwner);
            return true;
        } catch (Exception ex) {
            log.error("CDC event processing failed inside transaction: eventId={}, ledgerId={}, owner={}, cause={}",
                    eventLog.getEventId(), eventLog.getId(), processorOwner, ex.getMessage(), ex);
            handleCdcFailure(eventLog, ex);
            return false;
        }
    }

    @Transactional
    public int recoverStalledCdcEvents() {
        List<LedgerEventLog> stalled = eventLogRepository.findStalledCdcEvents(
                EventStatus.PROCESSING,
                cdcUtcSupport.utcDateTime(cdcUtcSupport.now().minusSeconds(600))
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

    private void handleCdcFailure(LedgerEventLog eventLog, Exception exception) {
        int nextRetryCount = (eventLog.getRetryCount() == null ? 0 : eventLog.getRetryCount()) + 1;
        if (nextRetryCount >= MAX_RETRY_COUNT) {
            int deadLettered = eventLogRepository.markCdcEventAsDeadLetter(
                    eventLog.getId(),
                    processorOwner,
                    EventStatus.DEAD_LETTER,
                    exception.getMessage(),
                    cdcUtcSupport.now(),
                    cdcUtcSupport.utcDateTime(cdcUtcSupport.now())
            );
            log.warn("CDC event moved to dead letter state: eventId={}, ledgerId={}, owner={}, retryCount={}, rowsUpdated={}, cause={}",
                    eventLog.getEventId(), eventLog.getId(), processorOwner, nextRetryCount, deadLettered, exception.getMessage());
            deadLetterQueueService.addCdcEventToDeadLetterQueue(eventLog, exception, nextRetryCount);
            return;
        }

        LocalDateTime nextRetryAt = cdcUtcSupport.utcDateTime(cdcUtcSupport.now().plusSeconds((long) Math.pow(2, nextRetryCount) * 30L));
        int rescheduled = eventLogRepository.rescheduleCdcEvent(
                eventLog.getId(),
                processorOwner,
                EventStatus.RETRY_SCHEDULED,
                exception.getMessage(),
                nextRetryCount,
                nextRetryAt,
                cdcUtcSupport.now(),
                cdcUtcSupport.utcDateTime(cdcUtcSupport.now())
        );
        log.warn("CDC event rescheduled for retry: eventId={}, ledgerId={}, owner={}, retryCount={}, nextRetryAt={}, rowsUpdated={}, cause={}",
                eventLog.getEventId(), eventLog.getId(), processorOwner, nextRetryCount, nextRetryAt, rescheduled, exception.getMessage());
    }

    private void handleWaitingOutcome(LedgerEventLog eventLog, CdcProcessingOutcome processingOutcome) {
        if (hasWaitingExceededSla(eventLog, processingOutcome.getLifecycleStatus())) {
            markCdcEventCancelled(
                    eventLog,
                    buildDependencyTimeoutMessage(eventLog, processingOutcome),
                    "DEPENDENCY_TIMEOUT",
                    processingOutcome.getLifecycleStatus()
            );
            return;
        }

        LocalDateTime nextRetryAt = calculateNextWaitingRetryAt(eventLog, processingOutcome.getLifecycleStatus());
        int deferred = eventLogRepository.markCdcEventAsWaiting(
                eventLog.getId(),
                processorOwner,
                EventStatus.WAITING_FOR_DEPENDENCY,
                processingOutcome.getMessage(),
                nextRetryAt,
                cdcUtcSupport.now(),
                cdcUtcSupport.utcDateTime(cdcUtcSupport.now()),
                cdcUtcSupport.utcDateTime(cdcUtcSupport.now())
        );
        log.info("CDC event moved to WAITING_FOR_DEPENDENCY: eventId={}, ledgerId={}, owner={}, nextRetryAt={}, rowsUpdated={}, offerStatus={}, reason={}",
                eventLog.getEventId(), eventLog.getId(), processorOwner, nextRetryAt, deferred,
                processingOutcome.getLifecycleStatus(), processingOutcome.getReasonCode());
    }

    private void handleCancelledOutcome(LedgerEventLog eventLog, CdcProcessingOutcome processingOutcome) {
        markCdcEventCancelled(
                eventLog,
                processingOutcome.getMessage(),
                processingOutcome.getReasonCode(),
                processingOutcome.getLifecycleStatus()
        );
    }

    private void markCdcEventCancelled(LedgerEventLog eventLog,
                                       String errorMessage,
                                       String reasonCode,
                                       String offerStatus) {
        int cancelled = eventLogRepository.markCdcEventAsCancelled(
                eventLog.getId(),
                processorOwner,
                EventStatus.CANCELLED,
                errorMessage,
                cdcUtcSupport.now(),
                cdcUtcSupport.utcDateTime(cdcUtcSupport.now()),
                cdcUtcSupport.utcDateTime(cdcUtcSupport.now())
        );
        log.info("CDC event cancelled without retry: eventId={}, ledgerId={}, owner={}, rowsUpdated={}, offerStatus={}, reasonCode={}, message={}",
                eventLog.getEventId(), eventLog.getId(), processorOwner, cancelled, offerStatus, reasonCode, errorMessage);
    }

    private boolean hasWaitingExceededSla(LedgerEventLog eventLog, String lifecycleStatus) {
        LocalDateTime referenceTime = eventLog.getCreatedAt();
        return referenceTime != null && referenceTime.isBefore(expirationThreshold(lifecycleStatus));
    }

    private LocalDateTime calculateNextWaitingRetryAt(LedgerEventLog eventLog, String lifecycleStatus) {
        LocalDateTime candidate = cdcUtcSupport.utcDateTime(cdcUtcSupport.now().plus(offerLifecycleProperties.waitingRetryInterval()));
        if (!offerLifecycleProperties.isCompletedStatus(lifecycleStatus) || eventLog.getCreatedAt() == null) {
            return candidate;
        }

        LocalDateTime completedDeadline = eventLog.getCreatedAt().plus(offerLifecycleProperties.completedReconciliationWindow());
        return candidate.isAfter(completedDeadline) ? completedDeadline : candidate;
    }

    private LocalDateTime expirationThreshold(String lifecycleStatus) {
        if (offerLifecycleProperties.isCompletedStatus(lifecycleStatus)) {
            return cdcUtcSupport.utcDateTime(cdcUtcSupport.now().minus(offerLifecycleProperties.completedReconciliationWindow()));
        }
        return cdcUtcSupport.utcDateTime(cdcUtcSupport.now().minus(offerLifecycleProperties.waitingWindow()));
    }

    private String buildDependencyTimeoutMessage(LedgerEventLog eventLog,
                                                 CdcProcessingOutcome processingOutcome) {
        if (offerLifecycleProperties.isCompletedStatus(processingOutcome.getLifecycleStatus())) {
            return "offer_letter_details completed-state reconciliation expired after "
                    + offerLifecycleProperties.getCompleted().getReconciliation().getMaxMinutes()
                    + " minute(s); cancelling orchestration cleanup"
                    + " [eventId=" + eventLog.getEventId()
                    + ", entityId=" + eventLog.getEntityId()
                    + ", offerStatus=" + processingOutcome.getLifecycleStatus()
                    + ", reason=COMPLETED_RECONCILIATION_TIMEOUT]";
        }
        return "offer_letter_details waiting expired after "
                + offerLifecycleProperties.getWaiting().getMaxDays()
                + " day(s); cancelling orchestration cleanup"
                + " [eventId=" + eventLog.getEventId()
                + ", entityId=" + eventLog.getEntityId()
                + ", offerStatus=" + processingOutcome.getLifecycleStatus()
                + ", reason=DEPENDENCY_TIMEOUT]";
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
                storeEventDateRange(event.getEventId(), startDate, endDate);
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

    private void storeEventDateRange(String eventId, LocalDate startDate, LocalDate endDate) {
        try {
            eventLogRepository.findByEventId(eventId).ifPresent(eventLog -> {
                eventLog.setPayload("{\"startDate\":\"" + startDate + "\",\"endDate\":\"" + endDate + "\"}");
                eventLogRepository.save(eventLog);
            });
        } catch (Exception e) {
            log.warn("Could not store date range for event {}: {}", eventId, e.getMessage());
        }
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
