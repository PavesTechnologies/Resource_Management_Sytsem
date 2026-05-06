package com.cdc.config;

import io.debezium.config.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

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
@org.springframework.context.annotation.Configuration
public class UnifiedDebeziumConfig {

    // PMS Configuration Properties
    @Value("${cdc.base.directory}")
    private String pmsCdcBaseDir;

    @Value("${cdc.database.type}")
    private String pmsDbType;

    @Value("${cdc.connector.class}")
    private String pmsConnectorClass;

    @Value("${cdc.database.hostname}")
    private String pmsDbHostname;

    @Value("${cdc.database.port}")
    private String pmsDbPort;

    @Value("${cdc.database.user}")
    private String pmsDbUser;

    @Value("${cdc.database.password}")
    private String pmsDbPassword;

    @Value("${cdc.database.name}")
    private String pmsDbName;

    @Value("${cdc.database.include.list}")
    private String pmsDatabaseIncludeList;

    @Value("${cdc.table.include.list}")
    private String pmsTableIncludeList;

    @Value("${cdc.server.name}")
    private String pmsServerName;

    @Value("${cdc.topic.prefix}")
    private String pmsTopicPrefix;

    // EOS Configuration Properties
    @Value("${eos.cdc.base.directory}")
    private String eosCdcBaseDir;

    @Value("${eos.cdc.database.type}")
    private String eosDbType;

    @Value("${eos.cdc.connector.class}")
    private String eosConnectorClass;

    @Value("${eos.cdc.database.hostname}")
    private String eosDbHostname;

    @Value("${eos.cdc.database.port}")
    private String eosDbPort;

    @Value("${eos.cdc.database.user}")
    private String eosDbUser;

    @Value("${eos.cdc.database.password}")
    private String eosDbPassword;

    @Value("${eos.cdc.database.name}")
    private String eosDbName;

    @Value("${eos.cdc.database.include.list}")
    private String eosDatabaseIncludeList;

    @Value("${eos.cdc.table.include.list}")
    private String eosTableIncludeList;

    @Value("${eos.cdc.server.name}")
    private String eosServerName;

    @Value("${eos.cdc.topic.prefix}")
    private String eosTopicPrefix;

    @Value("${eos.cdc.connector.name}")
    private String eosConnectorName;

    /**
     * PMS Debezium configuration bean.
     * Uses PMS-specific properties.
     */
    @Bean
    @Primary
    public Configuration debeziumConfiguration() {
        return createConfiguration(
                "pms-project-cdc",
                pmsConnectorClass,
                pmsCdcBaseDir,
                pmsDbType,
                pmsDbHostname,
                pmsDbPort,
                pmsDbUser,
                pmsDbPassword,
                pmsDbName,
                pmsDatabaseIncludeList,
                pmsTableIncludeList,
                pmsServerName,
                pmsTopicPrefix,
                5000,  // Server ID range start
                1000,  // Server ID offset
                15000, // Replica server ID range start
                "pms-project-offsets.dat",
                "pms-schema-history.dat",
                "always" // Snapshot mode
        );
    }

    /**
     * EOS Debezium configuration bean.
     * Uses EOS-specific properties.
     */
    @Bean("eosDebeziumConfiguration")
    public Configuration eosDebeziumConfiguration() {
        return createConfiguration(
                eosConnectorName,
                eosConnectorClass,
                eosCdcBaseDir,
                eosDbType,
                eosDbHostname,
                eosDbPort,
                eosDbUser,
                eosDbPassword,
                eosDbName,
                eosDatabaseIncludeList,
                eosTableIncludeList,
                eosServerName,
                eosTopicPrefix,
                25000, // Server ID range start (different from PMS)
                2000,  // Server ID offset (different from PMS)
                35000, // Replica server ID range start (different from PMS)
                "eos-offsets.dat",
                "eos-schema-history.dat",
                "when_needed" // Snapshot mode (different from PMS)
        );
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
            String snapshotMode
    ) {
        createDirIfMissing(baseDir);

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
                .with("snapshot.mode", snapshotMode)
                .with("snapshot.locking.mode", "minimal")
                .with("snapshot.fetch.size", "1024")
                .with("offset.storage", "org.apache.kafka.connect.storage.FileOffsetBackingStore")
                .with("offset.storage.file.filename", baseDir + "/" + offsetFileName)
                .with("schema.history.internal", "io.debezium.storage.file.history.FileSchemaHistory")
                .with("schema.history.internal.file.filename", baseDir + "/" + schemaHistoryFileName)
                .with("schema.history.internal.store.only.captured.tables.ddl", "true")
                .with("schema.history.internal.skip.unparseable.ddl", "true")
                .with("include.schema.changes", "false")

                // Handle binlog purging and GTID issues
                .with("gtid.source.includes", "")
                .with("binlog.buffer.size", "8192")
                .with("max.batch.size", "2048")
                .with("max.queue.size", "8192")

                // Disable JMX metrics to avoid registration conflicts
                .with("metrics.enabled", "false")
                .with("metrics.jmx.enabled", "false");

        // Add database-specific configuration
        addDatabaseSpecificConfig(configBuilder, dbType, dbName);

        return configBuilder.build();
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
     * Generate deterministic server ID based on hostname and port.
     * CRITICAL for multi-instance deployments to prevent Debezium server ID conflicts.
     * 
     * @param hostname Database hostname
     * @param port Database port  
     * @param baseRange Base range for server ID
     * @return Deterministic server ID
     */
    private String generateDeterministicServerId(String hostname, String port, int baseRange) {
        try {
            // Create deterministic hash from hostname:port combination
            String hostPort = hostname + ":" + port;
            int hash = Math.abs(hostPort.hashCode());
            
            // Generate server ID in the configured range
            // Ensures same hostname:port always gets same server ID
            int serverId = baseRange + (hash % 1000); // Use modulo to stay within reasonable range
            
            return String.valueOf(serverId);
        } catch (Exception e) {
            // Fallback to base range if anything goes wrong
            System.err.println("Failed to generate deterministic server ID, using fallback: " + e.getMessage());
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
