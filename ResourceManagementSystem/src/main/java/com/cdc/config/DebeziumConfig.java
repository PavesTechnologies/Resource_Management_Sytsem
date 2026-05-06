package com.cdc.config;

import io.debezium.config.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@org.springframework.context.annotation.Configuration
public class DebeziumConfig {

    @Value("${cdc.base.directory}")
    private String cdcBaseDir;

    @Value("${cdc.database.type}")
    private String dbType;

    @Value("${cdc.connector.class}")
    private String connectorClass;

    @Value("${cdc.database.hostname}")
    private String dbHostname;

    @Value("${cdc.database.port}")
    private String dbPort;

    @Value("${cdc.database.user}")
    private String dbUser;

    @Value("${cdc.database.password}")
    private String dbPassword;

    @Value("${cdc.database.name}")
    private String dbName;

    @Value("${cdc.database.include.list}")
    private String databaseIncludeList;

    @Value("${cdc.table.include.list}")
    private String tableIncludeList;

    @Value("${cdc.server.name}")
    private String serverName;

    @Value("${cdc.topic.prefix}")
    private String topicPrefix;

    @Bean
    public Configuration debeziumConfiguration() {

        createDirIfMissing(cdcBaseDir);

        Configuration.Builder configBuilder = Configuration.create()

                // Essential CDC configuration (from properties)
                .with("name", "pms-project-cdc")
                .with("connector.class", connectorClass)

                // Database connection (configurable based on type)
                .with("database.hostname", dbHostname)
                .with("database.port", dbPort)
                .with("database.user", dbUser)
                .with("database.password", dbPassword)

                // Server configuration (configurable)
                .with("database.server.id",
                        String.valueOf(Math.abs(new java.util.Random().nextInt(100_000)) + 5000))
                .with("database.server.name", serverName)
                .with("topic.prefix", topicPrefix)

                // Table capture (configurable)
                .with("database.include.list", databaseIncludeList)
                .with("table.include.list", tableIncludeList)

                // Fixed CDC settings (rarely change)
                .with("snapshot.mode", "when_needed")
                .with("offset.storage", "org.apache.kafka.connect.storage.FileOffsetBackingStore")
                .with("offset.storage.file.filename",
                        cdcBaseDir + "/pms-project-offsets.dat")
                .with("schema.history.internal", "io.debezium.storage.file.history.FileSchemaHistory")
                .with("schema.history.internal.file.filename",
                        cdcBaseDir + "/pms-schema-history.dat")
                .with("schema.history.internal.store.only.captured.tables.ddl", "true")
                .with("schema.history.internal.skip.unparseable.ddl", "true")
                .with("include.schema.changes", "false");

        // Add database-specific configuration
        addDatabaseSpecificConfig(configBuilder);

        return configBuilder.build();
    }

    private void addDatabaseSpecificConfig(Configuration.Builder configBuilder) {
        switch (dbType.toLowerCase()) {
            case "mysql":
                configBuilder.with("database.dbname", dbName);
                break;
            case "postgresql":
            case "postgres":
                configBuilder.with("database.dbname", dbName);
                configBuilder.with("plugin.name", "pgoutput");
                break;
            case "sqlserver":
                configBuilder.with("database.dbname", dbName);
                break;
            case "oracle":
                configBuilder.with("database.dbname", dbName);
                configBuilder.with("database.pdb.name", dbName);
                break;
            default:
                throw new IllegalArgumentException("Unsupported database type: " + dbType + 
                    ". Supported types: mysql, postgresql, sqlserver, oracle");
        }
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
