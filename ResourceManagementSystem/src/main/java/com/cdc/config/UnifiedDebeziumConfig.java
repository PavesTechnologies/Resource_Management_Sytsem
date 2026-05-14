package com.cdc.config;

import com.cdc.config.properties.CdcProperties;
import com.cdc.config.properties.EosCdcProperties;
import io.debezium.config.Configuration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * UNIFIED Debezium Configuration for BOTH PMS and EOS.
 * 
 * ELIMINATES code duplication by providing a generic configuration builder
 * that can create configurations for both PMS and EOS CDC systems.
 * 
 * Architecture:
 * - Generic configuration builder
 * - Configurable properties for each system
 * - Shared database-specific configuration logic
 * - Single source of truth for Debezium settings
 */
@Slf4j
@org.springframework.context.annotation.Configuration
@RequiredArgsConstructor
public class UnifiedDebeziumConfig {
    private final CdcProperties cdcProperties;
    private final EosCdcProperties eosCdcProperties;

    @Bean
    @Primary
    public Configuration debeziumConfiguration() {
        CdcProperties.DatabaseProperties database = cdcProperties.getDatabase();
        logConnectorStartMode("PMS", "pms-project-cdc", cdcProperties.getBaseDirectory(),
                "pms-project-offsets.dat", "pms-schema-history.dat");
        return createConfiguration(
                "pms-project-cdc",
                cdcProperties.getConnectorClass(),
                cdcProperties.getBaseDirectory(),
                database.getType(),
                database.getHostname(),
                String.valueOf(database.getPort()),
                database.getUser(),
                database.getPassword(),
                database.getName(),
                database.getIncludeList(),
                cdcProperties.getTableIncludeList(),
                cdcProperties.getServerName(),
                cdcProperties.getTopicPrefix(),
                5000,
                1000,
                15000,
                "pms-project-offsets.dat",
                "pms-schema-history.dat",
                cdcProperties.getSnapshotMode(),
                database.getSsl().getMode()
        );
    }

    @Bean("eosDebeziumConfiguration")
    public Configuration eosDebeziumConfiguration() {
        EosCdcProperties.DatabaseProperties database = eosCdcProperties.getDatabase();
        String baseDirectory = resolveEosBaseDirectory();
        logConnectorStartMode("EOS", eosCdcProperties.getConnectorName(), baseDirectory,
                "eos-offsets.dat", "eos-schema-history.dat");
        return createConfiguration(
                eosCdcProperties.getConnectorName(),
                eosCdcProperties.getConnectorClass(),
                baseDirectory,
                database.getType(),
                database.getHostname(),
                String.valueOf(database.getPort()),
                database.getUser(),
                database.getPassword(),
                database.getName(),
                database.getIncludeList(),
                eosCdcProperties.getTableIncludeList(),
                eosCdcProperties.getServerName(),
                eosCdcProperties.getTopicPrefix(),
                25000,
                2000,
                35000,
                "eos-offsets.dat",
                "eos-schema-history.dat",
                resolveEosSnapshotMode(),
                database.getSsl().getMode()
        );
    }

    private String resolveEosBaseDirectory() {
        return hasText(eosCdcProperties.getBaseDirectory())
                ? eosCdcProperties.getBaseDirectory()
                : cdcProperties.getBaseDirectory();
    }

    private String resolveEosSnapshotMode() {
        return hasText(eosCdcProperties.getSnapshotMode())
                ? eosCdcProperties.getSnapshotMode()
                : cdcProperties.getSnapshotMode();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private void logConnectorStartMode(String system, String connectorName, String baseDir,
                                       String offsetFile, String schemaFile) {
        String resolvedBaseDir = Paths.get(baseDir).toAbsolutePath().normalize().toString();
        Path offsetPath = Paths.get(resolvedBaseDir, offsetFile);
        Path schemaPath = Paths.get(resolvedBaseDir, schemaFile);
        boolean isFirstRun = !Files.exists(offsetPath) && !Files.exists(schemaPath);
        boolean recoveryRequired = requiresSchemaHistoryRecovery(offsetPath, schemaPath);
        log.info("{} CDC offset directory: {}", system, resolvedBaseDir);
        if (isFirstRun) {
            log.info("{} CDC starting initial snapshot for connector: {}", system, connectorName);
        } else if (recoveryRequired) {
            log.warn("{} CDC detected missing or unreadable schema history with preserved offsets; starting temporary schema_only_recovery for connector: {}",
                    system, connectorName);
        } else {
            log.info("{} CDC resuming incremental CDC from existing offsets for connector: {}", system, connectorName);
        }
    }

    /**
     * Generic configuration builder for both PMS and EOS.
     * Eliminates code duplication in configuration creation.
     */
    private Configuration createConfiguration(
            String connectorName,
            String connectorClass,
            String baseDir,
            String dbType,
            String dbHostname,
            String dbPort,
            String dbUser,
            String dbPassword,
            String dbName,
            String databaseIncludeList,
            String tableIncludeList,
            String serverName,
            String topicPrefix,
            int serverIdStart,
            int serverIdOffset,
            int replicaServerIdStart,
            String offsetFileName,
            String schemaHistoryFileName,
            String snapshotMode,
            String sslMode
    ) {
        // Resolve to absolute path so the engine finds the same offset files regardless
        // of which directory the JVM was launched from (IntelliJ vs Maven differ here)
        String resolvedBaseDir = Paths.get(baseDir).toAbsolutePath().normalize().toString();
        createDirIfMissing(resolvedBaseDir);

        Configuration.Builder configBuilder = Configuration.create()
                // Essential CDC configuration
                .with("name", connectorName)
                .with("connector.class", connectorClass)

                // Database connection
                .with("database.hostname", dbHostname)
                .with("database.port", dbPort)
                .with("database.user", dbUser)
                .with("database.password", dbPassword)

                // Server configuration - DETERMINISTIC server IDs for multi-instance safety
                .with("database.server.id", generateDeterministicServerId(dbHostname, dbPort, serverIdStart))
                .with("database.server.name", serverName)
                .with("database.server.id.offset", String.valueOf(serverIdOffset))
                .with("database.replica.server.id", generateDeterministicServerId(dbHostname, dbPort, replicaServerIdStart))
                .with("database.history.skip.unparseable.ddl", "true")
                .with("database.connection.attempts", "3")
                .with("database.connection.delay.ms", "1000")
                .with("topic.prefix", topicPrefix)

                // Table capture
                .with("database.include.list", databaseIncludeList)
                .with("table.include.list", tableIncludeList)

                // Fixed CDC settings
                .with("rms.configured.snapshot.mode", snapshotMode)
                .with("snapshot.mode", resolveSnapshotMode(resolvedBaseDir, offsetFileName, schemaHistoryFileName, snapshotMode))
                .with("snapshot.locking.mode", "minimal")
                .with("snapshot.fetch.size", "1024")
                .with("offset.storage", "org.apache.kafka.connect.storage.FileOffsetBackingStore")
                .with("offset.storage.file.filename", resolvedBaseDir + "/" + offsetFileName)
                .with("schema.history.internal", "io.debezium.storage.file.history.FileSchemaHistory")
                .with("schema.history.internal.file.filename", resolvedBaseDir + "/" + schemaHistoryFileName)
                .with("schema.history.internal.store.only.captured.tables.ddl", "true")
                .with("schema.history.internal.skip.unparseable.ddl", "true")
                .with("include.schema.changes", "false")

                // Flush offsets every 5 s so a clean shutdown preserves binlog position
                .with("offset.flush.interval.ms", "5000")
                .with("offset.flush.timeout.ms", "5000")

                // Performance
                .with("max.batch.size", "2048")
                .with("max.queue.size", "8192")

                // SSL - required for Aiven MySQL; plaintext connections are rejected
                .with("database.ssl.mode", sslMode);

        // Add database-specific configuration
        addDatabaseSpecificConfig(configBuilder, dbType, dbName);

        return configBuilder.build();
    }

    private String resolveSnapshotMode(String resolvedBaseDir,
                                       String offsetFileName,
                                       String schemaHistoryFileName,
                                       String configuredSnapshotMode) {
        Path offsetPath = Paths.get(resolvedBaseDir, offsetFileName);
        Path schemaHistoryPath = Paths.get(resolvedBaseDir, schemaHistoryFileName);

        if (requiresSchemaHistoryRecovery(offsetPath, schemaHistoryPath)) {
            return "schema_only_recovery";
        }

        return configuredSnapshotMode;
    }

    private boolean requiresSchemaHistoryRecovery(Path offsetPath, Path schemaHistoryPath) {
        if (!Files.exists(offsetPath)) {
            return false;
        }

        if (!Files.exists(schemaHistoryPath)) {
            return true;
        }

        try {
            return Files.size(schemaHistoryPath) == 0L;
        } catch (IOException ex) {
            log.warn("Unable to inspect schema history file {}. Falling back to schema recovery: {}",
                    schemaHistoryPath, ex.getMessage());
            return true;
        }
    }

    /**
     * Database-specific configuration logic.
     * Shared between PMS and EOS configurations.
     */
    private void addDatabaseSpecificConfig(Configuration.Builder configBuilder, String dbType, String dbName) {
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

    /**
     * Generate a server ID that is stable within one JVM process but unique across
     * concurrent processes (different PIDs). This prevents the MySQL error
     * "A replica with the same server_uuid/server_id has connected" that occurs when
     * a hot-reload or manual restart launches a second process before the first exits.
     */
    private String generateDeterministicServerId(String hostname, String port, int baseRange) {
        try {
            String hostPort = hostname + ":" + port;
            int hostHash = Math.abs(hostPort.hashCode()) % 500;
            int pidOffset = (int) (ProcessHandle.current().pid() % 500);
            int serverId = baseRange + hostHash + pidOffset;
            return String.valueOf(serverId);
        } catch (Exception e) {
            log.warn("Failed to generate server ID with PID, using base range: {}", e.getMessage());
            return String.valueOf(baseRange);
        }
    }

    private void createDirIfMissing(String dir) {
        try {
            Path path = Paths.get(dir);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create CDC directory: " + dir, e);
        }
    }
}
