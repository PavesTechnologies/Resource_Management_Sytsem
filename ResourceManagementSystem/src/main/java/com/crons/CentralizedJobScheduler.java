package com.crons;

import com.events.handler.DeadLetterQueueService;
import com.service_imple.JobLoggingService;
import com.service_imple.allocation_service_imple.AllocationClosureScheduler;
import com.service_imple.bench_service_impl.ResourceStateInitializationService;
import com.service_imple.ledger_service_impl.LedgerRetryService;
import com.service_imple.skill_service_impl.CertificateExpiryScheduler;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class CentralizedJobScheduler {

    // Unique node ID to track which instance ran the job in a cluster
    private static final String NODE_ID =
            System.getenv().getOrDefault("HOSTNAME", UUID.randomUUID().toString());

    // ── Inject all existing service beans ─────────────────────────────────
    private final CertificateExpiryScheduler certificateExpiryScheduler;
    private final AllocationClosureScheduler allocationClosureScheduler;
    private final LedgerRetryService ledgerRetryService;
    private final DeadLetterQueueService deadLetterQueueService;
    private final ResourceStateInitializationService resourceStateService;
    private final JobLoggingService jobLoggingService;

    public CentralizedJobScheduler(
            CertificateExpiryScheduler certificateExpiryScheduler,
            AllocationClosureScheduler allocationClosureScheduler,
            LedgerRetryService ledgerRetryService,
            DeadLetterQueueService deadLetterQueueService,
            ResourceStateInitializationService resourceStateService,
            JobLoggingService jobLoggingService) {
        this.certificateExpiryScheduler = certificateExpiryScheduler;
        this.allocationClosureScheduler = allocationClosureScheduler;
        this.ledgerRetryService = ledgerRetryService;
        this.deadLetterQueueService = deadLetterQueueService;
        this.resourceStateService = resourceStateService;
        this.jobLoggingService = jobLoggingService;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // BATCH 1 — Daily midnight: business logic jobs
    // ═══════════════════════════════════════════════════════════════════════

    @Scheduled(cron = "0 0 0 * * ?")
    @SchedulerLock(
            name            = "RMS_Daily_Midnight_Batch",
            lockAtLeastFor  = "PT1H",
            lockAtMostFor   = "PT6H"
    )
    public void runDailyMidnightBatch() {
        log.info("[{}] Starting RMS_Daily_Midnight_Batch", NODE_ID);
        runJob("CERTIFICATE-STATUS-UPDATE",
                certificateExpiryScheduler::updateStatuses);
        runJob("ALLOCATION-AUTO-CLOSURE",
                allocationClosureScheduler::processAutoClosures);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // BATCH 2 — Every 15 minutes: retry + DLQ processing
    // ═══════════════════════════════════════════════════════════════════════

    @Scheduled(fixedRate = 900_000)   // 15 minutes
    @SchedulerLock(
            name            = "RMS_Frequent_Retry_Job",
            lockAtLeastFor  = "PT5M",
            lockAtMostFor   = "PT15M"
    )
    public void runFrequentRetryJobs() {
        log.info("[{}] Starting RMS_Frequent_Retry_Job", NODE_ID);
        runJob("LEDGER-FAILED-EVENTS-RETRY",
                ledgerRetryService::processFailedEvents);
        runJob("DLQ-PROCESSING-LEDGER",
                ledgerRetryService::processDeadLetterQueue);
        runJob("DLQ-PROCESSING-DEADLETTER",
                deadLetterQueueService::processDeadLetterQueue);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // BATCH 3 — Nightly early-morning: cleanup + integrity checks
    // ═══════════════════════════════════════════════════════════════════════

    @Scheduled(cron = "0 0 2 * * ?")   // 2:00 AM
    @SchedulerLock(
            name            = "RMS_Nightly_Cleanup_Batch",
            lockAtLeastFor  = "PT5M",
            lockAtMostFor   = "PT30M"
    )
    public void runNightlyCleanupBatch() {
        log.info("[{}] Starting RMS_Nightly_Cleanup_Batch", NODE_ID);
        runJob("DLQ-CLEANUP-LEDGER",
                ledgerRetryService::cleanupOldDlqEntries);
        runJob("DLQ-CLEANUP-DEADLETTER",
                deadLetterQueueService::cleanupOldEntries);
        runJob("RESOURCE-STATE-CHECK",
                resourceStateService::dailyResourceStateCheck);
        runJob("DELETE-OLD-JOB-LOGS",
                () -> jobLoggingService.deleteOldLogs(30));
    }

    // NOTE: Event logs cleanup at 2:30 AM gets its own lock
    // so it doesn't compete with the 2:00 AM batch for the same lock name
    @Scheduled(cron = "0 30 2 * * ?")  // 2:30 AM
    @SchedulerLock(
            name            = "RMS_EventLogs_Cleanup",
            lockAtLeastFor  = "PT2M",
            lockAtMostFor   = "PT10M"
    )
    public void runEventLogsCleanup() {
        log.info("[{}] Starting RMS_EventLogs_Cleanup", NODE_ID);
        runJob("EVENT-LOGS-CLEANUP",
                ledgerRetryService::cleanupOldEventLogs);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Shared job runner — wraps every job with logging + error handling
    // ═══════════════════════════════════════════════════════════════════════

    private void runJob(String jobName, Runnable jobLogic) {
        UUID logId = jobLoggingService.createJobLog(jobName, NODE_ID);
        boolean success = false;
        String error = null;
        try {
            log.info("[{}] Executing job: {}", NODE_ID, jobName);
            jobLogic.run();
            success = true;
            log.info("[{}] Completed job: {}", NODE_ID, jobName);
        } catch (Exception e) {
            log.error("[{}] Job {} failed: {}", NODE_ID, jobName, e.getMessage(), e);
            error = e.getMessage();
        } finally {
            jobLoggingService.updateJobLog(logId, success, error);
        }
    }
}
