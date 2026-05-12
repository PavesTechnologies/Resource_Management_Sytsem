package com.cdc.retry;

import com.cdc.failure.CdcFailure;
import com.cdc.failure.CdcFailureRepository;
import com.cdc.service.EosDirectResyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UnifiedCdcRetryService {

    private final CdcFailureRepository cdcFailureRepository;
    private final EosDirectResyncService eosDirectResyncService;

    @Scheduled(fixedRate = 900000) // Every 15 minutes
    @SchedulerLock(name = "cdc-retry-failed-events", lockAtMostFor = "PT20M", lockAtLeastFor = "PT5M")
    @Transactional
    public void processFailedCdcEvents() {
        try {
            List<CdcFailure> failedEvents = cdcFailureRepository
                    .findByStatusAndNextRetryAtBefore("NEW", LocalDateTime.now().minusMinutes(15));

            for (CdcFailure event : failedEvents) {
                try {
                    retryFailedCdcEvent(event);
                } catch (Exception e) {
                    log.error("Failed to retry CDC event {} ({}): {}",
                            event.getEntityId(), event.getEntityType(), e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            log.error("Error processing failed CDC events: {}", e.getMessage(), e);
        }
    }

    @Scheduled(fixedRate = 900000) // Every 15 minutes
    @SchedulerLock(name = "cdc-retry-dlq-entries", lockAtMostFor = "PT20M", lockAtLeastFor = "PT5M")
    @Transactional
    public void processCdcDlqEntries() {
        try {
            List<CdcFailure> dlqEntries = cdcFailureRepository
                    .findByStatusAndNextRetryAtBefore("RETRY_EXHAUSTED", LocalDateTime.now());

            for (CdcFailure dlqEntry : dlqEntries) {
                try {
                    processCdcDlqEntry(dlqEntry);
                } catch (Exception e) {
                    log.error("Failed to process CDC DLQ entry {} ({}): {}",
                            dlqEntry.getEntityId(), dlqEntry.getEntityType(), e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            log.error("Error processing CDC DLQ: {}", e.getMessage(), e);
        }
    }

    @Transactional
    public void retryFailedCdcEvent(CdcFailure event) {
        event.setRetryCount(event.getRetryCount() + 1);
        event.setNextRetryAt(calculateNextRetryTime(event.getRetryCount()));

        if (event.getRetryCount() >= 3) {
            event.setStatus("PERMANENTLY_FAILED");
            cdcFailureRepository.save(event);
            return;
        }

        event.setStatus("PENDING");
        event.setErrorMessage(null);
        cdcFailureRepository.save(event);

        retryCdcEventProcessing(event);
    }

    @Transactional
    public void processCdcDlqEntry(CdcFailure dlqEntry) {
        try {
            retryCdcEventProcessing(dlqEntry);
            cdcFailureRepository.delete(dlqEntry);
            log.info("CDC DLQ entry processed successfully: {} ({})",
                    dlqEntry.getEntityId(), dlqEntry.getEntityType());

        } catch (Exception e) {
            dlqEntry.setRetryCount(dlqEntry.getRetryCount() + 1);
            dlqEntry.setErrorMessage(e.getMessage());
            dlqEntry.setNextRetryAt(calculateNextRetryTime(dlqEntry.getRetryCount()));

            if (dlqEntry.getRetryCount() >= 3) {
                dlqEntry.setStatus("RETRY_EXHAUSTED");
            }

            cdcFailureRepository.save(dlqEntry);
        }
    }

    // -------------------------------------------------------------------------
    // Retry routing
    // -------------------------------------------------------------------------

    private void retryCdcEventProcessing(CdcFailure event) {
        String entityType = event.getEntityType();

        if (entityType != null && entityType.startsWith("PMS-")) {
            retryPmsEventProcessing(event);
        } else if (entityType != null && entityType.startsWith("EOS-")) {
            retryEosEventProcessing(event);
        } else {
            log.warn("Unknown CDC entity type: {} for entityId: {}", entityType, event.getEntityId());
            event.setStatus("PERMANENTLY_FAILED");
            event.setErrorMessage("Unknown entity type: " + entityType);
            cdcFailureRepository.save(event);
        }
    }

    private void retryPmsEventProcessing(CdcFailure event) {
        // The stored payload is event.record().toString() — not a deserializable Struct.
        // True replay requires the original Debezium event; it cannot be reconstructed here.
        // Mark as PERMANENTLY_FAILED so the failure is visible and actionable.
        log.error("Cannot replay PMS CDC event - payload is not deserializable; "
                + "manual intervention required for entityType={}, entityId={}",
                event.getEntityType(), event.getEntityId());
        event.setStatus("PERMANENTLY_FAILED");
        event.setErrorMessage("Payload not deserializable; manual intervention required");
        cdcFailureRepository.save(event);
    }

    private void retryEosEventProcessing(CdcFailure event) {
        log.info("EOS CDC retry - re-fetching from EOS for entityType={}, entityId={}",
                event.getEntityType(), event.getEntityId());
        eosDirectResyncService.resync(event.getEntityType(), event.getEntityId());
        event.setStatus("RESOLVED");
        event.setErrorMessage(null);
        cdcFailureRepository.save(event);
        log.info("EOS CDC retry resolved for entityType={}, entityId={}",
                event.getEntityType(), event.getEntityId());
    }

    // -------------------------------------------------------------------------

    private LocalDateTime calculateNextRetryTime(int retryCount) {
        int delayMinutes = 15 * (int) Math.pow(2, retryCount);
        return LocalDateTime.now().plusMinutes(delayMinutes);
    }

    @Scheduled(cron = "0 0 2 * * ?") // 2 AM daily
    @Transactional
    public void cleanupOldCdcFailures() {
        try {
            LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
            int deleted = cdcFailureRepository.deleteOldFailures("RESOLVED", cutoff);
            log.info("Cleaned up {} old CDC failures (PMS + EOS)", deleted);
        } catch (Exception e) {
            log.error("Error cleaning up old CDC failures: {}", e.getMessage(), e);
        }
    }
}
