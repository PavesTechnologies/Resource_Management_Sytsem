package com.cdc.runner;

import com.cdc.service.CdcConnectorLeadershipService;
import io.debezium.embedded.Connect;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.RecordChangeEvent;
import io.debezium.engine.format.ChangeEventFormat;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.connect.source.SourceRecord;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Slf4j
public class UnifiedDebeziumRunner {

    private final ExecutorService executor;
    private final ScheduledExecutorService leadershipExecutor;

    private DebeziumEngine<RecordChangeEvent<SourceRecord>> engine;

    private final io.debezium.config.Configuration config;
    private final Consumer<RecordChangeEvent<SourceRecord>> eventHandler;
    private final String runnerName;
    private final boolean enabled;
    private final CdcConnectorLeadershipService leadershipService;

    private final Duration leadershipDuration = Duration.ofMinutes(2);

    private volatile boolean leadershipHeld;
    private volatile boolean engineStarted;
    private volatile boolean leadershipRenewalStarted;
    private volatile boolean schemaHistoryRecoveryAttempted;

    public UnifiedDebeziumRunner(io.debezium.config.Configuration config,
                                 Consumer<RecordChangeEvent<SourceRecord>> eventHandler,
                                 String runnerName,
                                 boolean enabled,
                                 CdcConnectorLeadershipService leadershipService) {

        this.config = config;
        this.eventHandler = eventHandler;
        this.runnerName = runnerName;
        this.enabled = enabled;
        this.leadershipService = leadershipService;

        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "debezium-engine-" + runnerName);

            t.setUncaughtExceptionHandler((thread, ex) ->
                    log.error(
                            "[{}] Debezium engine thread died unexpectedly: {}",
                            runnerName,
                            ex.getMessage(),
                            ex
                    )
            );

            return t;
        });

        this.leadershipExecutor = Executors.newSingleThreadScheduledExecutor(
                r -> new Thread(r, "cdc-leadership-" + runnerName)
        );
    }

    @EventListener(ApplicationReadyEvent.class)
    public void start() {

        if (!enabled) {
            log.info(
                    "[{}] CDC connector disabled via configuration; node is passive",
                    runnerName
            );
            return;
        }

        tryAcquireLeadershipAndStart();
    }

    @PreDestroy
    public synchronized void stop() throws IOException {

        leadershipRenewalStarted = false;

        leadershipExecutor.shutdownNow();

        stopEngineOnly();

        executor.shutdown();

        try {

            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {

                log.warn(
                        "[{}] Debezium engine shutdown timeout, forcing shutdown",
                        runnerName
                );

                executor.shutdownNow();
            }

        } catch (InterruptedException e) {

            log.warn(
                    "[{}] Debezium engine shutdown interrupted",
                    runnerName
            );

            executor.shutdownNow();

            Thread.currentThread().interrupt();
        }

        if (leadershipHeld) {

            leadershipService.release(lockName());

            leadershipHeld = false;
        }

        log.info("[{}] CDC runner stopped", runnerName);
    }

    private synchronized void tryAcquireLeadershipAndStart() {

        if (engineStarted) {
            return;
        }

        boolean acquired = leadershipService.tryAcquireLeadership(
                lockName(),
                leadershipDuration
        );

        if (!acquired) {

            leadershipHeld = false;

            log.info(
                    "[{}] Passive CDC node for {}. Leader={}",
                    runnerName,
                    lockName(),
                    leadershipService.currentLeader(lockName()).orElse("unknown")
            );

            return;
        }

        leadershipHeld = true;

        if (!leadershipRenewalStarted) {

            leadershipExecutor.scheduleAtFixedRate(
                    this::renewLeadership,
                    30,
                    30,
                    TimeUnit.SECONDS
            );

            leadershipRenewalStarted = true;
        }

        try {
            startEngine(config.asProperties(), false);

            log.info(
                    "[{}] Debezium engine started successfully under leadership {}",
                    runnerName,
                    leadershipService.ownerId()
            );

        } catch (Exception ex) {

            log.error(
                    "[{}] Debezium startup failed for {}: {}. Node will remain passive.",
                    runnerName,
                    lockName(),
                    ex.getMessage(),
                    ex
            );

            engineStarted = false;

            leadershipRenewalStarted = false;

            leadershipExecutor.shutdownNow();

            if (leadershipHeld) {

                leadershipService.release(lockName());

                leadershipHeld = false;
            }
        }
    }

    private synchronized void startEngine(Properties properties, boolean recoveryMode) {
        String offsetFile = properties.getProperty(
                "offset.storage.file.filename",
                "unknown"
        );
        String schemaHistoryFile = properties.getProperty(
                "schema.history.internal.file.filename",
                "unknown"
        );
        String snapshotMode = properties.getProperty("snapshot.mode", "unknown");

        if (recoveryMode) {
            log.warn(
                    "[{}] Restarting Debezium in temporary schema recovery mode. offsetFile={}, schemaHistoryFile={}, snapshotMode={}",
                    runnerName,
                    offsetFile,
                    schemaHistoryFile,
                    snapshotMode
            );
        } else {
            log.info(
                    "[{}] CDC leadership confirmed. Starting Debezium with offset file={}, schemaHistoryFile={}, snapshotMode={}",
                    runnerName,
                    offsetFile,
                    schemaHistoryFile,
                    snapshotMode
            );
        }

        engine = DebeziumEngine
                .create(ChangeEventFormat.of(Connect.class))
                .using(properties)
                .notifying(event -> {
                    try {
                        eventHandler.accept(event);
                    } catch (Exception ex) {
                        log.error(
                                "[{}] CDC handler failure",
                                runnerName,
                                ex
                        );
                    }
                })
                .using((success, message, error) -> {
                    engineStarted = false;

                    if (error != null) {
                        log.error(
                                "[{}] Debezium engine completed with error: {}",
                                runnerName,
                                error.getMessage(),
                                error
                        );

                        if (attemptSchemaHistoryRecovery(properties, error)) {
                            return;
                        }

                    } else if (success && message != null && message.contains("snapshot")) {

                        log.info(
                                "[{}] Initial snapshot completed successfully: {}",
                                runnerName,
                                message
                        );

                        log.info(
                                "[{}] Connector switched to realtime CDC streaming",
                                runnerName
                        );

                    } else {

                        log.info(
                                "[{}] Debezium engine completed. Success={} {}",
                                runnerName,
                                success,
                                message
                        );
                    }
                })
                .build();

        executor.execute(engine);

        engineStarted = true;
    }

    private synchronized boolean attemptSchemaHistoryRecovery(Properties failedProperties, Throwable error) {
        if (schemaHistoryRecoveryAttempted || !leadershipHeld || !isSchemaHistoryRecoveryCandidate(error)) {
            return false;
        }

        schemaHistoryRecoveryAttempted = true;

        String schemaHistoryFile = failedProperties.getProperty("schema.history.internal.file.filename");
        String configuredSnapshotMode = config.asProperties().getProperty("rms.configured.snapshot.mode", "when_needed");

        backupSchemaHistoryFile(schemaHistoryFile);

        Properties recoveryProperties = new Properties();
        recoveryProperties.putAll(config.asProperties());
        recoveryProperties.setProperty("snapshot.mode", "schema_only_recovery");

        log.warn(
                "[{}] Detected recoverable schema history failure. Preserving offsets and retrying once with schema_only_recovery. Normal snapshot mode will revert to {} on the next clean startup.",
                runnerName,
                configuredSnapshotMode
        );

        try {
            startEngine(recoveryProperties, true);
            return true;
        } catch (Exception recoveryStartFailure) {
            log.error(
                    "[{}] Debezium schema recovery restart failed: {}",
                    runnerName,
                    recoveryStartFailure.getMessage(),
                    recoveryStartFailure
            );
            return false;
        }
    }

    private boolean isSchemaHistoryRecoveryCandidate(Throwable error) {
        String message = error != null && error.getMessage() != null
                ? error.getMessage().toLowerCase()
                : "";

        return message.contains("db history topic is missing")
                || message.contains("schema history")
                || message.contains("schema isn't known to this connector")
                || message.contains("history topic is missing");
    }

    private void backupSchemaHistoryFile(String schemaHistoryFile) {
        if (schemaHistoryFile == null || schemaHistoryFile.isBlank()) {
            return;
        }

        try {
            Path schemaHistoryPath = Path.of(schemaHistoryFile);
            if (!Files.exists(schemaHistoryPath)) {
                log.warn("[{}] Schema history file is already missing at recovery time: {}",
                        runnerName, schemaHistoryPath);
                return;
            }

            Path backupPath = schemaHistoryPath.resolveSibling(
                    schemaHistoryPath.getFileName() + ".corrupt-" + DateTimeFormatter.ISO_INSTANT.format(Instant.now()).replace(":", "-") + ".bak"
            );
            Files.move(schemaHistoryPath, backupPath, StandardCopyOption.REPLACE_EXISTING);

            log.warn("[{}] Backed up schema history file before recovery: original={}, backup={}",
                    runnerName, schemaHistoryPath, backupPath);
        } catch (Exception ex) {
            log.warn("[{}] Failed to back up schema history file before recovery: {}",
                    runnerName, ex.getMessage(), ex);
        }
    }

    private void renewLeadership() {

        if (!leadershipHeld) {
            return;
        }

        boolean renewed = leadershipService.renewLeadership(
                lockName(),
                leadershipDuration
        );

        if (!renewed) {

            log.error(
                    "[{}] Lost CDC leadership for {}. Stopping connector and switching to passive mode.",
                    runnerName,
                    lockName()
            );

            leadershipHeld = false;

            leadershipRenewalStarted = false;

            leadershipExecutor.shutdownNow();

            try {

                stopEngineOnly();

            } catch (IOException e) {

                log.error(
                        "[{}] Failed to stop Debezium engine after leadership loss: {}",
                        runnerName,
                        e.getMessage(),
                        e
                );
            }
        }
    }

    private synchronized void stopEngineOnly() throws IOException {

        if (engine != null) {

            engine.close();

            engine = null;
        }

        engineStarted = false;
    }

    private String lockName() {
        return "CDC_CONNECTOR_" + runnerName;
    }
}
