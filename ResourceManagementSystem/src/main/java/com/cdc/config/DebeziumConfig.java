package com.cdc.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DebeziumConfig {

    @Bean
    public io.debezium.config.Configuration debeziumConfiguration() {

        return io.debezium.config.Configuration.create()

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


                // ONLY this table
                .with("database.include.list", "ajay")
                .with("table.include.list", "ajay.projects")

                // Snapshot is READ-ONLY
                .with("snapshot.mode", "initial")

                // Offset storage (restart safe)
                .with("offset.storage",
                        "org.apache.kafka.connect.storage.FileOffsetBackingStore")
                .with("offset.storage.file.filename",
                        "C:/debezium-offsets/pms-project-offsets.dat")

                .with("schema.history.internal",
                        "io.debezium.storage.file.history.FileSchemaHistory")
                .with("schema.history.internal.file.filename",
                        "C:/debezium-offsets/pms-schema-history.dat")

                .with("include.schema.changes", "false")

                .build();
    }
}

