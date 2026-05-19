package com.crons;

import com.cdc.retry.UnifiedCdcRetryService;
import com.events.handler.DeadLetterQueueService;
import com.events.handler.LedgerEventHandler;
import com.service_imple.allocation_service_imple.AllocationServiceImpl;
import com.service_imple.bench_service_impl.BenchService;
import com.service_imple.bench_service_impl.ResourceStateInitializationService;
import com.service_imple.ledger_service_impl.LedgerRetryService;
import com.service_imple.skill_service_impl.CertificateExpiryScheduler;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Single point of governance for all scheduled jobs in the RMS platform.
 *
 * Schedule map:
 *   00:00  RMS_Daily_Midnight_Batch     — certificate status, allocation auto-closure
 *   01:00  RMS_Nightly_Bench_Detection  — bench resource detection & state update
 *   02:00  RMS_Nightly_Cleanup_Batch    — DLQ cleanup, resource-state check, CDC failure cleanup, job-log purge
 *   02:30  RMS_EventLogs_Cleanup        — ledger event-log purge
 *   every-15m  RMS_Frequent_Retry_Job  — ledger/DLQ retries, CDC failure/DLQ retries
 *
 * Rules:
 *  - No @Scheduled annotation lives outside this class.
 *  - Every job runs through runJob() for uniform logging + error isolation.
 *  - Every batch method carries a ShedLock so only one node runs it in a cluster.
 */
@Component
@Slf4j
public class CentralizedJobScheduler {

    private static final String NODE_ID =
            System.getenv().getOrDefault("HOSTNAME", UUID.randomUUID().toString());

    // ── Injected service beans ────────────────────────────────────────────────
    private final CertificateExpiryScheduler certificateExpiryScheduler;
    private final AllocationServiceImpl allocationClosureScheduler;
    private final BenchService benchService;
    private final LedgerRetryService ledgerRetryService;
    private final DeadLetterQueueService deadLetterQueueService;
    private final UnifiedCdcRetryService unifiedCdcRetryService;
    private final LedgerEventHandler ledgerEventHandler;
    private final ResourceStateInitializationService resourceStateService;
    private final JobLoggingService jobLoggingService;

    public CentralizedJobScheduler(
            CertificateExpiryScheduler certificateExpiryScheduler,
            AllocationServiceImpl allocationClosureScheduler,
            BenchService benchService,
            LedgerRetryService ledgerRetryService,
            DeadLetterQueueService deadLetterQueueService,
            UnifiedCdcRetryService unifiedCdcRetryService,
            LedgerEventHandler ledgerEventHandler,
            ResourceStateInitializationService resourceStateService,
            JobLoggingService jobLoggingService) {
        this.certificateExpiryScheduler = certificateExpiryScheduler;
        this.allocationClosureScheduler = allocationClosureScheduler;
        this.benchService = benchService;
        this.ledgerRetryService = ledgerRetryService;
        this.deadLetterQueueService = deadLetterQueueService;
        this.unifiedCdcRetryService = unifiedCdcRetryService;
        this.ledgerEventHandler = ledgerEventHandler;
        this.resourceStateService = resourceStateService;
        this.jobLoggingService = jobLoggingService;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // BATCH 1 — Daily midnight: business-logic jobs
    // ═══════════════════════════════════════════════════════════════════════

    @Scheduled(cron = "0 0 0 * * ?")
    @SchedulerLock(
            name           = "RMS_Daily_Midnight_Batch",
            lockAtLeastFor = "PT1H",
            lockAtMostFor  = "PT6H"
    )
    public void runDailyMidnightBatch() {
        log.info("[{}] Starting RMS_Daily_Midnight_Batch", NODE_ID);
        runJob("CERTIFICATE-STATUS-UPDATE",
                certificateExpiryScheduler::updateStatuses);
        runJob("ALLOCATION-AUTO-CLOSURE",
                allocationClosureScheduler::processAutoClosures);
        runJob("PLANNED-ALLOCATION-ACTIVATION",
                allocationClosureScheduler::activatePlannedAllocations);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // BATCH 2 — 1:00 AM: nightly bench detection
    // ═══════════════════════════════════════════════════════════════════════

    @Scheduled(cron = "0 0 1 * * ?")
    @SchedulerLock(
            name           = "RMS_Nightly_Bench_Detection",
            lockAtLeastFor = "PT10M",
            lockAtMostFor  = "PT30M"
    )
    public void runNightlyBenchDetection() {
        log.info("[{}] Starting RMS_Nightly_Bench_Detection", NODE_ID);
        runJob("BENCH-RESOURCE-DETECTION",
                benchService::detectBenchResources);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // BATCH 3 — 2:00 AM: cleanup + integrity checks
    // ═══════════════════════════════════════════════════════════════════════

    @Scheduled(cron = "0 0 2 * * ?")
    @SchedulerLock(
            name           = "RMS_Nightly_Cleanup_Batch",
            lockAtLeastFor = "PT5M",
            lockAtMostFor  = "PT30M"
    )
    public void runNightlyCleanupBatch() {
        log.info("[{}] Starting RMS_Nightly_Cleanup_Batch", NODE_ID);
        runJob("DLQ-CLEANUP-LEDGER",
                ledgerRetryService::cleanupOldDlqEntries);
        runJob("DLQ-CLEANUP-DEADLETTER",
                deadLetterQueueService::cleanupOldEntries);
        runJob("CDC-CLEANUP",
                unifiedCdcRetryService::cleanupOldCdcFailures);
        runJob("RESOURCE-STATE-CHECK",
                resourceStateService::dailyResourceStateCheck);
        runJob("DELETE-OLD-JOB-LOGS",
                () -> jobLoggingService.deleteOldLogs(30));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // BATCH 4 — 2:30 AM: event-log purge (separate lock — doesn't race BATCH 3)
    // ═══════════════════════════════════════════════════════════════════════

    @Scheduled(cron = "0 30 2 * * ?")
    @SchedulerLock(
            name           = "RMS_EventLogs_Cleanup",
            lockAtLeastFor = "PT2M",
            lockAtMostFor  = "PT10M"
    )
    public void runEventLogsCleanup() {
        log.info("[{}] Starting RMS_EventLogs_Cleanup", NODE_ID);
        runJob("EVENT-LOGS-CLEANUP",
                ledgerRetryService::cleanupOldEventLogs);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // BATCH 5 — Every 15 minutes: retry + DLQ processing
    // ═══════════════════════════════════════════════════════════════════════

    // lockAtMostFor must be LONGER than the worst-case job duration, not just the interval.
    // Old CDC lock used PT20M; we keep that ceiling here so a slow CDC/ledger retry
    // does not allow a second node to start the same batch before the first finishes.
    @Scheduled(fixedRate = 900_000)
    @SchedulerLock(
            name           = "RMS_Frequent_Retry_Job",
            lockAtLeastFor = "PT5M",
            lockAtMostFor  = "PT20M"
    )
    public void runFrequentRetryJobs() {
        log.info("[{}] Starting RMS_Frequent_Retry_Job", NODE_ID);
        runJob("LEDGER-FAILED-EVENTS-RETRY",
                ledgerRetryService::processFailedEvents);
        runJob("DLQ-PROCESSING-LEDGER",
                ledgerRetryService::processDeadLetterQueue);
        runJob("DLQ-PROCESSING-DEADLETTER",
                deadLetterQueueService::processDeadLetterQueue);
        runJob("CDC-FAILED-EVENTS-RETRY",
                unifiedCdcRetryService::processFailedCdcEvents);
        runJob("CDC-DLQ-RETRY",
                unifiedCdcRetryService::processCdcDlqEntries);
    }

    @Scheduled(fixedDelayString = "${cdc.inbox.poll-interval-ms:5000}")
    @SchedulerLock(
            name = "RMS_CDC_Inbox_Processor",
            lockAtLeastFor = "PT1S",
            lockAtMostFor = "PT30S"
    )
    public void runCdcInboxProcessor() {
        runJob("CDC-INBOX-PROCESSOR",
                () -> ledgerEventHandler.processPendingCdcEvents(25));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Shared job runner — uniform logging + per-job error isolation
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
