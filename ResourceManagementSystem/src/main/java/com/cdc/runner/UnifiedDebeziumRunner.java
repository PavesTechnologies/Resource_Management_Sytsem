package com.cdc.runner;

import io.debezium.embedded.Connect;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.RecordChangeEvent;
import io.debezium.engine.format.ChangeEventFormat;
import org.apache.kafka.connect.source.SourceRecord;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
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

    private final Executor executor = Executors.newSingleThreadExecutor();
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
    }

    /**
     * Start the Debezium engine with the configured handler.
     */
    @PostConstruct
    public void start() {
        engine = DebeziumEngine
                .create(ChangeEventFormat.of(Connect.class))
                .using(config.asProperties())
                .notifying(eventHandler)
                .build();

        executor.execute(engine);
        
        System.out.println(runnerName + " Debezium Engine started successfully");
    }

    /**
     * Stop the Debezium engine gracefully.
     */
    @PreDestroy
    public void stop() throws IOException {
        if (engine != null) {
            engine.close();
            System.out.println(runnerName + " Debezium Engine stopped successfully");
        }
    }
}
