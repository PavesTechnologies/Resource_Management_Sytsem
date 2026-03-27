package com.service_imple.ledger_service_impl;

import com.entity.ledger_entities.LedgerEventLog;
import com.entity.ledger_entities.DeadLetterQueue;
import com.entity_enums.ledger_enums.DLQStatus;
import com.entity_enums.ledger_enums.EventStatus;
import com.repo.ledger_repo.LedgerEventLogRepository;
import com.repo.ledger_repo.DeadLetterQueueRepository;
import com.service_interface.ledger_service_interface.AvailabilityCalculationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 🔐 STEP 5: RETRY + DLQ SERVICE
 * 
 * Handles retry processing for failed ledger events:
 * - Processes dead letter queue every 15 minutes
 * - Implements exponential backoff
 * - Max 3 retries per event
 * - Moves permanently failed events to manual review
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LedgerRetryService {

    private final LedgerEventLogRepository ledgerEventLogRepository;
    private final DeadLetterQueueRepository deadLetterQueueRepository;
    private final AvailabilityCalculationService availabilityCalculationService;

    /**
     * 🔐 STEP 5: RETRY + DLQ - Process failed events every 15 minutes
     */
    @Scheduled(fixedRate = 900000) // 15 minutes in milliseconds
    @Transactional
    public void processFailedEvents() {
        log.info("Starting processing of failed ledger events");
        
        try {
            List<LedgerEventLog> failedEvents = ledgerEventLogRepository
                    .findRetryableEvents(EventStatus.FAILED, 3, LocalDateTime.now().minusMinutes(15));
            
            if (failedEvents.isEmpty()) {
                log.debug("No failed events to retry");
                return;
            }
            
            log.info("Found {} failed events to retry", failedEvents.size());
            
            for (LedgerEventLog event : failedEvents) {
                try {
                    retryFailedEvent(event);
                } catch (Exception e) {
                    log.error("Failed to retry event {}: {}", event.getEventId(), e.getMessage(), e);
                }
            }
            
        } catch (Exception e) {
            log.error("Error processing failed events: {}", e.getMessage(), e);
        }
        
        log.info("Completed processing of failed ledger events");
    }

    /**
     * 🔐 STEP 5: RETRY + DLQ - Process dead letter queue
     */
    @Scheduled(fixedRate = 900000) // 15 minutes
    @Transactional
    public void processDeadLetterQueue() {
        log.info("Starting processing of dead letter queue");
        
        try {
            List<DeadLetterQueue> dlqEntries = deadLetterQueueRepository
                    .findRetryableEvents(DLQStatus.PENDING_RETRY, LocalDateTime.now());
            
            if (dlqEntries.isEmpty()) {
                log.debug("No DLQ entries to process");
                return;
            }
            
            log.info("Found {} DLQ entries to process", dlqEntries.size());
            
            for (DeadLetterQueue dlqEntry : dlqEntries) {
                try {
                    processDlqEntry(dlqEntry);
                } catch (Exception e) {
                    log.error("Failed to process DLQ entry {}: {}", 
                             dlqEntry.getEventId(), e.getMessage(), e);
                }
            }
            
        } catch (Exception e) {
            log.error("Error processing dead letter queue: {}", e.getMessage(), e);
        }
        
        log.info("Completed processing of dead letter queue");
    }

    /**
     * 🔐 STEP 5: RETRY + DLQ - Retry individual failed event
     */
    @Transactional
    public void retryFailedEvent(LedgerEventLog event) {
        log.info("Retrying failed event {} (attempt {})", 
                 event.getEventId(), event.getRetryCount() + 1);
        
        // Update retry count and next retry time
        event.setRetryCount(event.getRetryCount() + 1);
        event.setNextRetryAt(calculateNextRetryTime(event.getRetryCount()));
        
        if (event.getRetryCount() >= 3) {
            log.warn("Event {} reached max retries, marking as permanently failed", event.getEventId());
            event.setStatus(EventStatus.PERMANENTLY_FAILED);
            ledgerEventLogRepository.save(event);
            return;
        }
        
        // Reset status to PENDING for retry
        event.setStatus(EventStatus.PENDING);
        event.setErrorMessage(null);
        ledgerEventLogRepository.save(event);
        
        // Trigger retry by calling the idempotent calculation method
        availabilityCalculationService.recalculateDailyWithIdempotency(
                event.getResourceId(), 
                event.getCreatedAt().toLocalDate(), 
                event.getEventId()
        );
    }

    /**
     * 🔐 STEP 5: RETRY + DLQ - Process individual DLQ entry
     */
    @Transactional
    public void processDlqEntry(DeadLetterQueue dlqEntry) {
        log.info("Processing DLQ entry {} (attempt {})", 
                 dlqEntry.getEventId(), dlqEntry.getRetryCount() + 1);
        
        try {
            // Retry the calculation
            availabilityCalculationService.recalculateDailyWithIdempotency(
                    dlqEntry.getResourceId(), 
                    dlqEntry.getEventDate().toLocalDate(), 
                    dlqEntry.getEventId()
            );
            
            // If successful, remove from DLQ
            deadLetterQueueRepository.delete(dlqEntry);
            log.info("Successfully processed DLQ entry {}, removed from queue", dlqEntry.getEventId());
            
        } catch (Exception e) {
            // Update DLQ entry for next retry
            dlqEntry.setRetryCount(dlqEntry.getRetryCount() + 1);
            dlqEntry.setErrorMessage(e.getMessage());
            dlqEntry.setNextRetryAt(calculateNextRetryTime(dlqEntry.getRetryCount()));
            
            if (dlqEntry.getRetryCount() >= 3) {
                log.warn("DLQ entry {} reached max retries, marking for manual review", dlqEntry.getEventId());
                dlqEntry.setStatus(DLQStatus.RETRY_EXHAUSTED);
            }
            
            deadLetterQueueRepository.save(dlqEntry);
        }
    }

    /**
     * 🔐 STEP 5: RETRY + DLQ - Calculate next retry time with exponential backoff
     */
    private LocalDateTime calculateNextRetryTime(int retryCount) {
        // Exponential backoff: 15min, 30min, 60min
        int delayMinutes = 15 * (int) Math.pow(2, retryCount);
        return LocalDateTime.now().plusMinutes(delayMinutes);
    }

    /**
     * 🔐 STEP 5: RETRY + DLQ - Clean up old DLQ entries
     */
    @Scheduled(cron = "0 0 2 * * ?") // Daily at 2 AM
    @Transactional
    public void cleanupOldDlqEntries() {
        log.info("Starting cleanup of old DLQ entries");
        
        try {
            LocalDateTime cutoff = LocalDateTime.now().minusDays(7); // Keep for 7 days
            int deleted = deadLetterQueueRepository.deleteOldEntries(DLQStatus.PERMANENTLY_FAILED, cutoff);
            
            log.info("Cleaned up {} old DLQ entries older than {}", deleted, cutoff);
            
        } catch (Exception e) {
            log.error("Error cleaning up old DLQ entries: {}", e.getMessage(), e);
        }
    }

    /**
     * 🔐 STEP 5: RETRY + DLQ - Clean up old event logs
     */
    @Scheduled(cron = "0 30 2 * * ?") // Daily at 2:30 AM
    @Transactional
    public void cleanupOldEventLogs() {
        log.info("Starting cleanup of old event logs");
        
        try {
            LocalDateTime cutoff = LocalDateTime.now().minusDays(30); // Keep for 30 days
            int deleted = ledgerEventLogRepository.deleteOldEvents(cutoff);
            
            log.info("Cleaned up {} old event logs older than {}", deleted, cutoff);
            
        } catch (Exception e) {
            log.error("Error cleaning up old event logs: {}", e.getMessage(), e);
        }
    }
}
