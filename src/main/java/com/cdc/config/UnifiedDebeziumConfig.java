package com.cdc.config;

import com.cdc.config.properties.CdcProperties;
import com.cdc.config.properties.EosCdcProperties;
import io.debezium.config.Configuration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@Slf4j
@org.springframework.context.annotation.Configuration
@RequiredArgsConstructor
public class UnifiedDebeziumConfig {

    private final CdcProperties cdcProperties;
    private final EosCdcProperties eosCdcProperties;

    @Value("${spring.datasource.url}")
    private String rmsDbUrl;

    @Value("${spring.datasource.username}")
    private String rmsDbUser;

    @Value("${spring.datasource.password}")
    private String rmsDbPassword;

    @Bean
    @Primary
    public Configuration debeziumConfiguration() {
        CdcProperties.DatabaseProperties database = cdcProperties.getDatabase();
        log.info("PMS CDC starting with JDBC offset storage (offsetTable=debezium_pms_offsets, schemaHistoryTable=debezium_pms_schema_history)");
        return createConfiguration(
                "pms-project-cdc",
                cdcProperties.getConnectorClass(),
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
                "debezium_pms_offsets",
                "debezium_pms_schema_history",
                cdcProperties.getSnapshotMode(),
                database.getSsl().getMode()
        );
    }

    @Bean("eosDebeziumConfiguration")
    public Configuration eosDebeziumConfiguration() {
        EosCdcProperties.DatabaseProperties database = eosCdcProperties.getDatabase();
        String snapshotMode = resolveEosSnapshotMode();
        log.info("EOS CDC starting with JDBC offset storage (offsetTable=debezium_eos_offsets, schemaHistoryTable=debezium_eos_schema_history)");
        return createConfiguration(
                eosCdcProperties.getConnectorName(),
                eosCdcProperties.getConnectorClass(),
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
                "debezium_eos_offsets",
                "debezium_eos_schema_history",
                snapshotMode,
                database.getSsl().getMode()
        );
    }

    private String resolveEosSnapshotMode() {
        return hasText(eosCdcProperties.getSnapshotMode())
                ? eosCdcProperties.getSnapshotMode()
                : cdcProperties.getSnapshotMode();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private Configuration createConfiguration(
            String connectorName,
            String connectorClass,
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
            String offsetTableName,
            String schemaHistoryTableName,
            String snapshotMode,
            String sslMode
    ) {
        Configuration.Builder configBuilder = Configuration.create()
                .with("name", connectorName)
                .with("connector.class", connectorClass)

                // Source database connection
                .with("database.hostname", dbHostname)
                .with("database.port", dbPort)
                .with("database.user", dbUser)
                .with("database.password", dbPassword)

                // Deterministic server IDs — stable per JVM process, unique across concurrent processes
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

                // Snapshot
                .with("rms.configured.snapshot.mode", snapshotMode)
                .with("snapshot.mode", snapshotMode)
                .with("snapshot.locking.mode", "minimal")
                .with("snapshot.fetch.size", "1024")

                // JDBC offset storage — persists binlog position in the RMS database;
                // survives pod restarts and requires no PVC or host-mounted volume
                .with("offset.storage", "io.debezium.storage.jdbc.offset.JdbcOffsetBackingStore")
                .with("offset.storage.jdbc.url", rmsDbUrl)
                .with("offset.storage.jdbc.user", rmsDbUser)
                .with("offset.storage.jdbc.password", rmsDbPassword)
                .with("offset.storage.jdbc.offset.table.name", offsetTableName)

                // JDBC schema history — persists DDL history in the RMS database
                .with("schema.history.internal", "io.debezium.storage.jdbc.history.JdbcSchemaHistory")
                .with("schema.history.internal.jdbc.url", rmsDbUrl)
                .with("schema.history.internal.jdbc.user", rmsDbUser)
                .with("schema.history.internal.jdbc.password", rmsDbPassword)
                .with("schema.history.internal.jdbc.schema.history.table.name", schemaHistoryTableName)
                .with("schema.history.internal.store.only.captured.tables.ddl", "true")
                .with("schema.history.internal.skip.unparseable.ddl", "true")
                .with("include.schema.changes", "false")

                // Flush offsets every 5 s so a clean shutdown preserves binlog position
                .with("offset.flush.interval.ms", "5000")
                .with("offset.flush.timeout.ms", "5000")

                // Performance
                .with("max.batch.size", "2048")
                .with("max.queue.size", "8192")

                .with("database.ssl.mode", sslMode);

        addDatabaseSpecificConfig(configBuilder, dbType, dbName);

        return configBuilder.build();
    }

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
}
