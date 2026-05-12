package com.cdc.runner;

import io.debezium.embedded.Connect;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.RecordChangeEvent;
import io.debezium.engine.format.ChangeEventFormat;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.connect.source.SourceRecord;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * UNIFIED Debezium Runner for BOTH PMS and EOS CDC.
 * 
 * ELIMINATES code duplication by providing a generic runner that can handle
 * both PMS and EOS CDC handlers with the same lifecycle management.
 * 
 * Architecture:
 * - Generic runner implementation
 * - Configurable handler injection
 * - Shared lifecycle management
 * - Single source of truth for Debezium engine management
 * - Enterprise observability and logging
 */
@Slf4j
public class UnifiedDebeziumRunner {

    private final ExecutorService executor;
    private DebeziumEngine<RecordChangeEvent<SourceRecord>> engine;

    private final io.debezium.config.Configuration config;
    private final Consumer<RecordChangeEvent<SourceRecord>> eventHandler;
    private final String runnerName;

    /**
     * Generic constructor for any CDC handler.
     * 
     * @param config Debezium configuration
     * @param eventHandler CDC event handler (PMS or EOS)
     * @param runnerName Name for logging/identification
     */
    public UnifiedDebeziumRunner(io.debezium.config.Configuration config,
                                 Consumer<RecordChangeEvent<SourceRecord>> eventHandler,
                                 String runnerName) {
        this.config = config;
        this.eventHandler = eventHandler;
        this.runnerName = runnerName;
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "debezium-engine-" + runnerName);
            t.setUncaughtExceptionHandler((thread, ex) ->
                log.error("[{}] Debezium engine thread died unexpectedly: {}", runnerName, ex.getMessage(), ex));
            return t;
        });
    }

    @PostConstruct
    public void start() {
        String offsetFile = config.asProperties().getProperty("offset.storage.file.filename", "unknown");
        log.info("[{}] CDC ENGINE STARTUP — offset file: {}", runnerName, offsetFile);
        
        engine = DebeziumEngine
                .create(ChangeEventFormat.of(Connect.class))
                .using(config.asProperties())
                .notifying(eventHandler)
                .using((success, message, error) -> {
                    if (error != null) {
                        log.error("[{}] Debezium engine completed with error: {}", runnerName, error.getMessage(), error);
                    } else {
                        if (success && message.contains("snapshot")) {
                            log.info("[{}] Initial snapshot completed successfully: {}", runnerName, message);
                            log.info("[{}] Connector switched to realtime CDC streaming", runnerName);
                        } else {
                            log.info("[{}] Debezium engine completed. Success={} {}", runnerName, success, message);
                        }
                    }
                })
                .build();

        executor.execute(engine);
        log.info("[{}] Debezium engine started successfully", runnerName);
    }

    @PreDestroy
    public void stop() throws IOException {
        log.info("ENTERPRISE CDC ENGINE SHUTDOWN - Connector: {}", runnerName);
        
        if (engine != null) {
            engine.close();
        }
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                log.warn("[{}] Debezium engine shutdown timeout, forcing shutdown", runnerName);
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            log.warn("[{}] Debezium engine shutdown interrupted", runnerName);
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("[{}] Debezium engine stopped successfully", runnerName);
    }
}
