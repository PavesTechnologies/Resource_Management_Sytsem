package com.events.handler;

import com.entity.ledger_entities.DeadLetterQueue;
import com.entity_enums.ledger_enums.DLQStatus;
import com.events.ledger_events.BaseLedgerEvent;
import com.events.publisher.LedgerEventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.repo.ledger_repo.DeadLetterQueueRepository;
import com.repo.ledger_repo.LedgerEventLogRepository;
import com.service_interface.ledger_service_interface.AvailabilityCalculationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeadLetterQueueService {

    private final DeadLetterQueueRepository deadLetterQueueRepository;
    private final LedgerEventLogRepository ledgerEventLogRepository;
    private final ObjectMapper objectMapper;
    private final LedgerEventPublisher ledgerEventPublisher;
    private final AvailabilityCalculationService availabilityCalculationService;

    private static final int MAX_RETRY_COUNT = 3;
    private static final int RETRY_DELAY_MINUTES = 15;
    private static final int MAX_AGE_DAYS = 30;

    @Transactional
    public void addToDeadLetterQueue(BaseLedgerEvent event, Exception exception, int retryCount) {
        try {
            String eventId = event.getEventId();
            String payload = objectMapper.writeValueAsString(event);
            String errorMessage = exception.getMessage();

            DeadLetterQueue dlqEntry = DeadLetterQueue.builder()
                    .eventId(eventId)
                    .payload(payload)
                    .errorMessage(errorMessage)
                    .retryCount(retryCount)
                    .maxRetryCount(MAX_RETRY_COUNT)
                    .status(DLQStatus.PENDING_RETRY)
                    .nextRetryAt(LocalDateTime.now().plusMinutes(RETRY_DELAY_MINUTES))
                    .originalEventType(event.getEventType())
                    .resourceId(event.getResourceId())
                    .build();

            deadLetterQueueRepository.save(dlqEntry);

            log.info("Added event {} to dead letter queue, retry count: {}", eventId, retryCount);

        } catch (Exception e) {
            log.error("Failed to add event {} to dead letter queue: {}", event.getEventId(), e.getMessage());
        }
    }

//    @Scheduled(fixedDelay = 15 * 60 * 1000) // Every 15 minutes
    @Transactional
    public void processDeadLetterQueue() {
        try {
            List<DeadLetterQueue> readyForRetry = deadLetterQueueRepository.findReadyForRetry(
                    DLQStatus.PENDING_RETRY, LocalDateTime.now());

            if (readyForRetry.isEmpty()) {
                return;
            }

            log.info("Found {} DLQ entries ready for retry", readyForRetry.size());

            for (DeadLetterQueue dlqEntry : readyForRetry) {
                processDLQEntry(dlqEntry);
            }

            cleanupOldEntries();

        } catch (Exception e) {
            log.error("Error processing dead letter queue: {}", e.getMessage());
        }
    }

    @Transactional
    public void retryLedgerCalculation(DeadLetterQueue dlqEntry) {
        try {
            Map<String, Object> payload = objectMapper.readValue(dlqEntry.getPayload(), Map.class);
            
            Long resourceId = ((Number) payload.get("resourceId")).longValue();
            String startDateStr = (String) payload.get("startDate");
            String endDateStr = (String) payload.get("endDate");
            
            LocalDate startDate = LocalDate.parse(startDateStr);
            LocalDate endDate = LocalDate.parse(endDateStr);
            
            log.info("Retrying ledger calculation for resource {} from {} to {}", resourceId, startDate, endDate);
            
            availabilityCalculationService.recalculateForDateRange(resourceId, startDate, endDate);
            
            dlqEntry.setStatus(DLQStatus.MANUALLY_PROCESSED);
            dlqEntry.setUpdatedAt(LocalDateTime.now());
            deadLetterQueueRepository.save(dlqEntry);
            
            log.info("Successfully retried ledger calculation for resource {}", resourceId);
            
        } catch (Exception e) {
            log.error("Failed to retry ledger calculation for DLQ entry {}: {}", 
                    dlqEntry.getEventId(), e.getMessage());
            
            int newRetryCount = dlqEntry.getRetryCount() + 1;
            dlqEntry.setRetryCount(newRetryCount);
            dlqEntry.setLastRetryAt(LocalDateTime.now());
            dlqEntry.setErrorMessage(e.getMessage());

            if (newRetryCount >= dlqEntry.getMaxRetryCount()) {
                dlqEntry.setStatus(DLQStatus.RETRY_EXHAUSTED);
                dlqEntry.setNextRetryAt(null);
            } else {
                dlqEntry.setStatus(DLQStatus.PENDING_RETRY);
                long delayMinutes = (long) RETRY_DELAY_MINUTES * (long) Math.pow(2, newRetryCount - 1);
                dlqEntry.setNextRetryAt(LocalDateTime.now().plusMinutes(delayMinutes));
            }

            deadLetterQueueRepository.save(dlqEntry);
        }
    }

    @Transactional
    public void processDLQEntry(DeadLetterQueue dlqEntry) {
        try {
            // Check if this is a ledger calculation payload
            if (dlqEntry.getOriginalEventType() != null && 
                (dlqEntry.getOriginalEventType().equals("ALLOCATION_UPDATE") ||
                 dlqEntry.getOriginalEventType().equals("RANGE_UPDATE") ||
                 dlqEntry.getOriginalEventType().equals("RESOURCE_UPDATE") ||
                 dlqEntry.getOriginalEventType().equals("SYNC_UPDATE"))) {
                
                retryLedgerCalculation(dlqEntry);
                return;
            }
            
            // Handle event-based payloads
            BaseLedgerEvent event = deserializeEvent(dlqEntry.getPayload());
            
            if (event == null) {
                markDLQEntryAsExhausted(dlqEntry.getId(), "Failed to deserialize event");
                return;
            }

            if (dlqEntry.getRetryCount() >= dlqEntry.getMaxRetryCount()) {
                markDLQEntryAsExhausted(dlqEntry.getId(), "Maximum retry count exceeded");
                return;
            }

            log.info("Retrying event {} from DLQ, attempt {}", dlqEntry.getEventId(), dlqEntry.getRetryCount() + 1);

            ledgerEventPublisher.validateAndPublish(event);

            dlqEntry.setStatus(DLQStatus.MANUALLY_PROCESSED);
            dlqEntry.setUpdatedAt(LocalDateTime.now());
            deadLetterQueueRepository.save(dlqEntry);

            log.info("Successfully retried event {} from DLQ", dlqEntry.getEventId());

        } catch (Exception e) {
            log.error("Failed to retry DLQ entry {}: {}", dlqEntry.getEventId(), e.getMessage());
            
            int newRetryCount = dlqEntry.getRetryCount() + 1;
            dlqEntry.setRetryCount(newRetryCount);
            dlqEntry.setLastRetryAt(LocalDateTime.now());
            dlqEntry.setErrorMessage(e.getMessage());

            if (newRetryCount >= dlqEntry.getMaxRetryCount()) {
                dlqEntry.setStatus(DLQStatus.RETRY_EXHAUSTED);
                dlqEntry.setNextRetryAt(null);
            } else {
                dlqEntry.setStatus(DLQStatus.PENDING_RETRY);
                long delayMinutes = (long) RETRY_DELAY_MINUTES * (long) Math.pow(2, newRetryCount - 1);
                dlqEntry.setNextRetryAt(LocalDateTime.now().plusMinutes(delayMinutes));
            }

            deadLetterQueueRepository.save(dlqEntry);
        }
    }

    @Transactional
    public void markDLQEntryAsExhausted(Long dlqId, String reason) {
        try {
            DeadLetterQueue dlqEntry = deadLetterQueueRepository.findById(dlqId).orElse(null);
            if (dlqEntry != null) {
                dlqEntry.setStatus(DLQStatus.RETRY_EXHAUSTED);
                dlqEntry.setErrorMessage(reason);
                dlqEntry.setNextRetryAt(null);
                dlqEntry.setUpdatedAt(LocalDateTime.now());
                deadLetterQueueRepository.save(dlqEntry);
                
                log.warn("DLQ entry {} marked as exhausted: {}", dlqId, reason);
            }
        } catch (Exception e) {
            log.error("Failed to mark DLQ entry {} as exhausted: {}", dlqId, e.getMessage());
        }
    }

//    @Scheduled(cron = "0 0 2 * * ?") // Daily at 2 AM
    @Transactional
    public void cleanupOldEntries() {
        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(MAX_AGE_DAYS);
            
            List<DeadLetterQueue> oldEntries = deadLetterQueueRepository.findOldEntries(cutoffDate, null).getContent();
            
            for (DeadLetterQueue entry : oldEntries) {
                if (entry.getStatus() == DLQStatus.RETRY_EXHAUSTED || 
                    entry.getStatus() == DLQStatus.MANUALLY_PROCESSED ||
                    entry.getStatus() == DLQStatus.PERMANENTLY_FAILED) {
                    
                    deadLetterQueueRepository.deleteById(entry.getId());
                }
            }

            log.info("Cleaned up old DLQ entries older than {} days", MAX_AGE_DAYS);

        } catch (Exception e) {
            log.error("Failed to cleanup old DLQ entries: {}", e.getMessage());
        }
    }

    private BaseLedgerEvent deserializeEvent(String payload) {
        try {
            return objectMapper.readValue(payload, BaseLedgerEvent.class);
        } catch (Exception e) {
            log.error("Failed to deserialize event from DLQ payload: {}", e.getMessage());
            return null;
        }
    }

    @Transactional(readOnly = true)
    public List<DeadLetterQueue> getDLQEntriesForResource(Long resourceId) {
        return deadLetterQueueRepository.findByResourceId(resourceId);
    }

    @Transactional(readOnly = true)
    public List<DeadLetterQueue> getDLQEntriesByStatus(DLQStatus status) {
        return deadLetterQueueRepository.findByStatus(status);
    }

    @Transactional
    public void manuallyProcessDLQEntry(Long dlqId, String processedBy) {
        try {
            DeadLetterQueue dlqEntry = deadLetterQueueRepository.findById(dlqId).orElse(null);
            if (dlqEntry != null) {
                BaseLedgerEvent event = deserializeEvent(dlqEntry.getPayload());
                if (event != null) {
                    ledgerEventPublisher.validateAndPublish(event);
                    
                    dlqEntry.setStatus(DLQStatus.MANUALLY_PROCESSED);
                    dlqEntry.setUpdatedAt(LocalDateTime.now());
                    deadLetterQueueRepository.save(dlqEntry);
                    
                    log.info("Manually processed DLQ entry {} by {}", dlqId, processedBy);
                } else {
                    markDLQEntryAsExhausted(dlqId, "Failed to deserialize event during manual processing");
                }
            }
        } catch (Exception e) {
            log.error("Failed to manually process DLQ entry {}: {}", dlqId, e.getMessage());
        }
    }

    @Transactional
    public void permanentlyFailDLQEntry(Long dlqId, String reason) {
        try {
            DeadLetterQueue dlqEntry = deadLetterQueueRepository.findById(dlqId).orElse(null);
            if (dlqEntry != null) {
                dlqEntry.setStatus(DLQStatus.PERMANENTLY_FAILED);
                dlqEntry.setErrorMessage(reason);
                dlqEntry.setNextRetryAt(null);
                dlqEntry.setUpdatedAt(LocalDateTime.now());
                deadLetterQueueRepository.save(dlqEntry);
                
                log.info("Permanently failed DLQ entry {}: {}", dlqId, reason);
            }
        } catch (Exception e) {
            log.error("Failed to permanently fail DLQ entry {}: {}", dlqId, e.getMessage());
        }
    }
}
