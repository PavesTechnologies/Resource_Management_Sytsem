package com.service_imple.ledger_service_impl;

import com.entity.ledger_entities.LedgerEventLog;
import com.entity.ledger_entities.DeadLetterQueue;
import com.entity_enums.ledger_enums.DLQStatus;
import com.entity_enums.ledger_enums.EventStatus;
import com.repo.ledger_repo.LedgerEventLogRepository;
import com.repo.ledger_repo.DeadLetterQueueRepository;
import com.service_interface.ledger_service_interface.LedgerAvailabilityCalculationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LedgerRetryService {

    private final LedgerEventLogRepository ledgerEventLogRepository;
    private final DeadLetterQueueRepository deadLetterQueueRepository;
    private final LedgerAvailabilityCalculationService availabilityCalculationService;

//    @Scheduled(fixedRate = 900000)
    @Transactional
    public void processFailedEvents() {
        try {
            List<LedgerEventLog> failedEvents = ledgerEventLogRepository
                    .findRetryableEvents(EventStatus.FAILED, 3, LocalDateTime.now().minusMinutes(15));
            
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
    }

//    @Scheduled(fixedRate = 900000)
    @Transactional
    public void processDeadLetterQueue() {
        try {
            List<DeadLetterQueue> dlqEntries = deadLetterQueueRepository
                    .findRetryableEvents(DLQStatus.PENDING_RETRY, LocalDateTime.now());
            
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
    }

    @Transactional
    public void retryFailedEvent(LedgerEventLog event) {
        event.setRetryCount(event.getRetryCount() + 1);
        event.setNextRetryAt(calculateNextRetryTime(event.getRetryCount()));
        
        if (event.getRetryCount() >= 3) {
            event.setStatus(EventStatus.PERMANENTLY_FAILED);
            ledgerEventLogRepository.save(event);
            return;
        }
        
        event.setStatus(EventStatus.PENDING);
        event.setErrorMessage(null);
        ledgerEventLogRepository.save(event);
        
        availabilityCalculationService.recalculateDailyWithIdempotency(
                event.getResourceId(), 
                event.getCreatedAt().toLocalDate(), 
                event.getEventId()
        );
    }

    @Transactional
    public void processDlqEntry(DeadLetterQueue dlqEntry) {
        try {
            availabilityCalculationService.recalculateDailyWithIdempotency(
                    dlqEntry.getResourceId(), 
                    dlqEntry.getEventDate().toLocalDate(), 
                    dlqEntry.getEventId()
            );
            
            deadLetterQueueRepository.delete(dlqEntry);
            
        } catch (Exception e) {
            dlqEntry.setRetryCount(dlqEntry.getRetryCount() + 1);
            dlqEntry.setErrorMessage(e.getMessage());
            dlqEntry.setNextRetryAt(calculateNextRetryTime(dlqEntry.getRetryCount()));
            
            if (dlqEntry.getRetryCount() >= 3) {
                dlqEntry.setStatus(DLQStatus.RETRY_EXHAUSTED);
            }
            
            deadLetterQueueRepository.save(dlqEntry);
        }
    }

    private LocalDateTime calculateNextRetryTime(int retryCount) {
        int delayMinutes = 15 * (int) Math.pow(2, retryCount);
        return LocalDateTime.now().plusMinutes(delayMinutes);
    }

//    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupOldDlqEntries() {
        try {
            LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
            int deleted = deadLetterQueueRepository.deleteOldEntries(DLQStatus.PERMANENTLY_FAILED, cutoff);
        } catch (Exception e) {
            log.error("Error cleaning up old DLQ entries: {}", e.getMessage(), e);
        }
    }

//    @Scheduled(cron = "0 30 2 * * ?")
    @Transactional
    public void cleanupOldEventLogs() {
        try {
            LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
            int deleted = ledgerEventLogRepository.deleteOldEvents(cutoff);
        } catch (Exception e) {
            log.error("Error cleaning up old event logs: {}", e.getMessage(), e);
        }
    }
}
