package com.cdc.config;

import io.debezium.config.Configuration;
import org.springframework.context.annotation.Bean;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@org.springframework.context.annotation.Configuration
public class DebeziumConfig {

    private static final String CDC_BASE_DIR =
            System.getProperty("user.home") + "/rms-cdc";

    @Bean
    public Configuration debeziumConfiguration() {

        createDirIfMissing(CDC_BASE_DIR);

        return Configuration.create()

                .with("name", "pms-project-cdc")

                .with("connector.class",
                        "io.debezium.connector.mysql.MySqlConnector")

                // PMS MySQL
                .with("database.hostname", "pms-db-service-ruchithacloud-9d59.c.aivencloud.com")
                .with("database.port", "14189")
                .with("database.user", "avnadmin")
                .with("database.password", "AVNS_GAUyWTQz-MGNiSAVpPS")

                .with("database.server.id", "607724949")
                .with("database.server.name", "pms_mysql")
                .with("topic.prefix", "pms")

                // Capture ONLY required table
                .with("database.include.list", "ajay")
                .with("table.include.list", "ajay.projects")

                // ✅ PERMANENT FIX
                .with("snapshot.mode", "when_needed")

                // Offset storage (dynamic & team-safe)
                .with("offset.storage",
                        "org.apache.kafka.connect.storage.FileOffsetBackingStore")
                .with("offset.storage.file.filename",
                        CDC_BASE_DIR + "/pms-project-offsets.dat")

                // Schema history (file-based, no Kafka)
                .with("schema.history.internal",
                        "io.debezium.storage.file.history.FileSchemaHistory")
                .with("schema.history.internal.file.filename",
                        CDC_BASE_DIR + "/pms-schema-history.dat")

                .with("schema.history.internal.store.only.captured.tables.ddl", "true")
                .with("schema.history.internal.skip.unparseable.ddl", "true")

                .with("include.schema.changes", "false")

                .build();
    }

    private void createDirIfMissing(String dir) {
        try {
            Path path = Paths.get(dir);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create CDC directory", e);
        }
    }
}
