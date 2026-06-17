package com.cdc.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Pre-creates Debezium JDBC offset and schema-history tables before the Debezium
 * engine starts. Runs at HIGHEST_PRECEDENCE so it always completes before
 * UnifiedDebeziumRunner listeners fire.
 *
 * Debezium 3.2.0.Final issues solved:
 * 1. Auto-DDL uses VARCHAR(65000) for history_data — rejected by MySQL under utf8mb4.
 *    Pre-creating with LONGTEXT bypasses this.
 * 2. Schema history table needs BOTH history_data_seq (used in INSERT) and
 *    record_insert_seq (used in SELECT ORDER BY). If either is missing the table
 *    is dropped and recreated automatically.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DebeziumJdbcTableInitializer implements ApplicationListener<ApplicationReadyEvent>, Ordered {

    private final JdbcTemplate jdbcTemplate;

    // record_insert_seq is used by JdbcOffsetBackingStore ORDER BY clause
    // record_insert_ts / record_update_ts need DEFAULT CURRENT_TIMESTAMP because
    // Debezium's INSERT does not always supply record_update_ts explicitly —
    // MySQL strict mode rejects NOT NULL columns with no default when omitted
    private static final String OFFSET_DDL = """
            CREATE TABLE IF NOT EXISTS %s (
                id                VARCHAR(36)  NOT NULL,
                offset_key        VARCHAR(1255),
                offset_val        VARCHAR(1255),
                record_insert_seq INTEGER,
                record_insert_ts  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                record_update_ts  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                PRIMARY KEY (id)
            )
            """;

    // history_data_seq is used by JdbcSchemaHistory INSERT
    // record_insert_seq is used by JdbcSchemaHistory SELECT ORDER BY
    // Both are required — missing either one causes a SQLSyntaxErrorException
    // history_data uses LONGTEXT because Debezium's auto-DDL VARCHAR(65000) is
    // rejected by MySQL under utf8mb4 charset
    private static final String SCHEMA_HISTORY_DDL = """
            CREATE TABLE IF NOT EXISTS %s (
                id                VARCHAR(36) NOT NULL,
                history_data      LONGTEXT,
                history_data_seq  INTEGER,
                record_insert_seq INTEGER,
                record_insert_ts  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
                record_update_ts  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                PRIMARY KEY (id)
            )
            """;

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        ensureTable("debezium_pms_offsets",        OFFSET_DDL,         "offset_key", "record_insert_seq");
        ensureTable("debezium_eos_offsets",        OFFSET_DDL,         "offset_key", "record_insert_seq");
        ensureTable("debezium_pms_schema_history", SCHEMA_HISTORY_DDL, "history_data_seq", "record_insert_seq");
        ensureTable("debezium_eos_schema_history", SCHEMA_HISTORY_DDL, "history_data_seq", "record_insert_seq");
    }

    private void ensureTable(String tableName, String ddl, String... requiredColumns) {
        try {
            if (!hasAllColumns(tableName, requiredColumns)) {
                jdbcTemplate.execute("DROP TABLE IF EXISTS " + tableName);
                jdbcTemplate.execute(String.format(ddl, tableName));
                log.info("Debezium JDBC table created: {}", tableName);
            } else {
                // Table exists with correct columns — patch timestamp defaults in place
                // so existing tables created without DEFAULT CURRENT_TIMESTAMP are fixed
                // without dropping any data
                jdbcTemplate.execute("ALTER TABLE " + tableName
                        + " MODIFY COLUMN record_insert_ts TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP"
                        + ", MODIFY COLUMN record_update_ts TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");
                log.info("Debezium JDBC table ready: {}", tableName);
            }
        } catch (Exception ex) {
            log.error("Failed to initialize Debezium JDBC table {}: {}", tableName, ex.getMessage(), ex);
        }
    }

    private boolean hasAllColumns(String tableName, String[] columns) {
        try {
            jdbcTemplate.execute(
                    "SELECT " + String.join(", ", columns) + " FROM " + tableName + " WHERE 1 = 0"
            );
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}
