package com.cdc.runner;

import com.cdc.listener.ProjectCdcHandler;

import io.debezium.embedded.Connect;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.RecordChangeEvent;
import io.debezium.engine.format.ChangeEventFormat;
import org.apache.kafka.connect.source.SourceRecord;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Component
public class DebeziumRunner {

    private final Executor executor = Executors.newSingleThreadExecutor();
    private DebeziumEngine<RecordChangeEvent<SourceRecord>> engine;

    private final io.debezium.config.Configuration config;
    private final ProjectCdcHandler projectCdcHandler;

    public DebeziumRunner(io.debezium.config.Configuration config,
                          ProjectCdcHandler projectCdcHandler) {
        this.config = config;
        this.projectCdcHandler = projectCdcHandler;
    }

    @PostConstruct
    public void start() {

        engine = DebeziumEngine
                .create(ChangeEventFormat.of(Connect.class))
                .using(config.asProperties())
                .notifying(projectCdcHandler::handleEvent) // 🔥 THIS LINE
                .build();

        executor.execute(engine);
    }

    @PreDestroy
    public void stop() throws IOException {
        if (engine != null) {
            engine.close();
        }
    }
}
