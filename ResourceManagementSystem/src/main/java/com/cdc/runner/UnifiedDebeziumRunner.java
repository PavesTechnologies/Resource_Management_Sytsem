package com.cdc.runner;

import io.debezium.embedded.Connect;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.RecordChangeEvent;
import io.debezium.engine.format.ChangeEventFormat;
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
 */
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
                System.err.println("[" + runnerName + "] Debezium engine thread died unexpectedly: " + ex.getMessage()));
            return t;
        });
    }

    @PostConstruct
    public void start() {
        engine = DebeziumEngine
                .create(ChangeEventFormat.of(Connect.class))
                .using(config.asProperties())
                .notifying(eventHandler)
                .using((success, message, error) -> {
                    if (error != null) {
                        System.err.println("[" + runnerName + "] Debezium engine completed with error: " + error.getMessage());
                    } else {
                        System.out.println("[" + runnerName + "] Debezium engine completed. Success=" + success + " " + message);
                    }
                })
                .build();

        executor.execute(engine);
        System.out.println("[" + runnerName + "] Debezium engine started");
    }

    @PreDestroy
    public void stop() throws IOException {
        if (engine != null) {
            engine.close();
        }
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        System.out.println("[" + runnerName + "] Debezium engine stopped");
    }
}
