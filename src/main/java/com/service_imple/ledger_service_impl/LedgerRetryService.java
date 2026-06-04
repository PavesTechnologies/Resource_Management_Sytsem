package com.service_imple.ledger_service_impl;

import com.entity.ledger_entities.LedgerEventLog;
import com.entity.ledger_entities.DeadLetterQueue;
import com.entity_enums.ledger_enums.DLQStatus;
import com.entity_enums.ledger_enums.EventStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.repo.ledger_repo.LedgerEventLogRepository;
import com.repo.ledger_repo.DeadLetterQueueRepository;
import com.service_interface.ledger_service_interface.LedgerAvailabilityCalculationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LedgerRetryService {

    private final LedgerEventLogRepository ledgerEventLogRepository;
    private final DeadLetterQueueRepository deadLetterQueueRepository;
    private final LedgerAvailabilityCalculationService availabilityCalculationService;
    private final ObjectMapper objectMapper;

    @Transactional
    public void processFailedEvents() {
        try {
            List<LedgerEventLog> failedEvents = ledgerEventLogRepository
                    .findRetryableEvents(EventStatus.RETRY_SCHEDULED, 3, LocalDateTime.now());
            
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

        event.setStatus(EventStatus.NEW);
        event.setErrorMessage(null);
        ledgerEventLogRepository.save(event);

        LocalDate[] range = extractDateRangeFromPayload(event);
        availabilityCalculationService.recalculateForDateRange(
                event.getResourceId(),
                range[0],
                range[1]
        );
    }

    private LocalDate[] extractDateRangeFromPayload(LedgerEventLog event) {
        try {
            if (event.getPayload() != null && !event.getPayload().isBlank()) {
                JsonNode node = objectMapper.readTree(event.getPayload());
                LocalDate start = LocalDate.parse(node.get("startDate").asText());
                LocalDate end = LocalDate.parse(node.get("endDate").asText());
                return new LocalDate[]{start, end};
            }
        } catch (Exception e) {
            log.warn("Could not parse date range from payload for event {}, falling back to createdAt: {}",
                    event.getEventId(), e.getMessage());
        }
        // fallback: recalculate the single day the event was created on
        LocalDate fallback = event.getCreatedAt().toLocalDate();
        return new LocalDate[]{fallback, fallback};
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
            int deleted = ledgerEventLogRepository.deleteOldNonCdcEvents(cutoff);
            log.info("Deleted {} non-CDC ledger_event_log row(s) older than {}", deleted, cutoff);
        } catch (Exception e) {
            log.error("Error cleaning up old event logs: {}", e.getMessage(), e);
        }
    }
}
