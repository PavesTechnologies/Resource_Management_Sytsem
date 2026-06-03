package com.cdc.service;

import com.zaxxer.hikari.HikariDataSource;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@Slf4j
public class CdcConnectorLeadershipService {

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;
    private final PlatformTransactionManager transactionManager;
    private final TransactionTemplate writeTransactionTemplate;
    private final MeterRegistry meterRegistry;
    private final String ownerId = System.getenv().getOrDefault("HOSTNAME", UUID.randomUUID().toString());
    private final ConcurrentMap<String, Boolean> leadershipStates = new ConcurrentHashMap<>();

    public CdcConnectorLeadershipService(@Qualifier("cdcLeadershipJdbcTemplate") JdbcTemplate jdbcTemplate,
                                         @Qualifier("dataSource") DataSource dataSource,
                                         @Qualifier("cdcLeadershipTransactionManager") PlatformTransactionManager transactionManager,
                                         MeterRegistry meterRegistry) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
        this.transactionManager = transactionManager;
        this.meterRegistry = meterRegistry;
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        template.setReadOnly(false);
        this.writeTransactionTemplate = template;
    }

    @PostConstruct
    void registerMetrics() {
        Gauge.builder("rms.cdc.leadership.connectors", leadershipStates::size)
                .description("Number of CDC connectors tracked for leadership")
                .register(meterRegistry);

        logLeadershipWiring();
    }

    @Transactional(
            transactionManager = "cdcLeadershipTransactionManager",
            propagation = Propagation.REQUIRES_NEW,
            readOnly = false
    )
    public boolean tryAcquireLeadership(String connectorName, Duration lockDuration) {

        try {

            forceWriteConnection();

            boolean acquired =
                    acquireOrRenewInternal(connectorName, lockDuration, false);

            leadershipStates.put(connectorName, acquired);

            if (acquired) {
                log.info("[{}] CDC leadership acquired for {}", ownerId, connectorName);
            } else {
                log.info("[{}] CDC leadership unavailable for {}. Current leader={}",
                        ownerId,
                        connectorName,
                        currentLeader(connectorName).orElse("unknown"));
            }

            return acquired;

        } catch (Exception ex) {

            leadershipStates.put(connectorName, false);

            log.error("[{}] CDC leadership acquisition failed for {}: {}",
                    ownerId,
                    connectorName,
                    ex.getMessage(),
                    ex);

            return false;
        }
    }

    @Transactional(
            transactionManager = "cdcLeadershipTransactionManager",
            propagation = Propagation.REQUIRES_NEW,
            readOnly = false
    )
    public boolean renewLeadership(String connectorName, Duration lockDuration) {
        try {
            boolean renewed = Boolean.TRUE.equals(writeTransactionTemplate.execute(status -> {
                forceWriteConnection();
                return acquireOrRenewInternal(connectorName, lockDuration, true);
            }));

            leadershipStates.put(connectorName, renewed);
            if (renewed) {
                log.debug("[{}] CDC leadership renewed for {}", ownerId, connectorName);
            } else {
                log.warn("[{}] CDC leadership renewal failed for {}. Current leader={}",
                        ownerId, connectorName, currentLeader(connectorName).orElse("unknown"));
            }
            return renewed;
        } catch (Exception ex) {
            leadershipStates.put(connectorName, false);
            log.error("[{}] CDC leadership renewal error for {}: {}",
                    ownerId, connectorName, ex.getMessage(), ex);
            return false;
        }
    }

    @Transactional(
            transactionManager = "cdcLeadershipTransactionManager",
            propagation = Propagation.REQUIRES_NEW,
            readOnly = false
    )
    public void release(String connectorName) {
        try {
            writeTransactionTemplate.executeWithoutResult(status -> {
                forceWriteConnection();
                Instant now = Instant.now();
                jdbcTemplate.update("""
                        UPDATE shedlock
                           SET lock_until = ?, locked_at = ?, locked_by = ?
                         WHERE name = ?
                           AND locked_by = ?
                        """,
                        Timestamp.from(now),
                        Timestamp.from(now),
                        ownerId,
                        connectorName,
                        ownerId
                );
            });
            leadershipStates.put(connectorName, false);
            log.info("[{}] CDC leadership released for {}", ownerId, connectorName);
        } catch (Exception ex) {
            leadershipStates.put(connectorName, false);
            log.warn("[{}] Unable to release CDC leadership for {}: {}", ownerId, connectorName, ex.getMessage());
        }
    }

    public Optional<String> currentLeader(String connectorName) {
        try {
            return jdbcTemplate.query("""
                            SELECT locked_by
                              FROM shedlock
                             WHERE name = ?
                               AND lock_until > ?
                        """,
                    rs -> rs.next() ? Optional.ofNullable(rs.getString("locked_by")) : Optional.empty(),
                    connectorName,
                    Timestamp.from(Instant.now())
            );
        } catch (DataAccessException ex) {
            log.debug("Unable to fetch current CDC leader for {}: {}", connectorName, ex.getMessage());
            return Optional.empty();
        }
    }

    public boolean isLeader(String connectorName) {
        return Boolean.TRUE.equals(leadershipStates.get(connectorName));
    }

    public String ownerId() {
        return ownerId;
    }

    private boolean acquireOrRenewInternal(String connectorName, Duration lockDuration, boolean renewalOnly) {
        Instant now = Instant.now();
        Instant lockUntil = now.plus(lockDuration);

        int updated = jdbcTemplate.update("""
                UPDATE shedlock
                   SET lock_until = ?, locked_at = ?, locked_by = ?
                 WHERE name = ?
                   AND (lock_until IS NULL OR lock_until <= ? OR locked_by = ?)
                """,
                Timestamp.from(lockUntil),
                Timestamp.from(now),
                ownerId,
                connectorName,
                Timestamp.from(now),
                ownerId
        );
        if (updated > 0) {
            return true;
        }

        if (renewalOnly) {
            return false;
        }

        try {
            int inserted = jdbcTemplate.update("""
                    INSERT INTO shedlock(name, lock_until, locked_at, locked_by)
                    VALUES (?, ?, ?, ?)
                    """,
                    connectorName,
                    Timestamp.from(lockUntil),
                    Timestamp.from(now),
                    ownerId
            );
            return inserted > 0;
        } catch (DataAccessException ex) {
            log.debug("CDC leadership row already exists for {}: {}", connectorName, ex.getMessage());
            return false;
        }
    }

    private void forceWriteConnection() {
        try {
            Connection connection = DataSourceUtils.getConnection(dataSource);
            if (connection.isReadOnly()) {
                log.error("CDC leadership obtained read-only connection. {}",
                        describeConnection(connection));
                connection.setReadOnly(false);
                if (connection.isReadOnly()) {
                    throw new IllegalStateException("CDC leadership transaction received a read-only connection");
                }
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to obtain write-capable connection for CDC leadership", ex);
        }
    }

    private void logLeadershipWiring() {
        log.info(
                "CDC leadership wiring: jdbcTemplateBean=cdcLeadershipJdbcTemplate, jdbcTemplateDataSourceClass={}, jdbcTemplateDataSourceId={}, dataSourceBean=dataSource, dataSourceClass={}, dataSourceId={}, transactionManagerBean=cdcLeadershipTransactionManager, transactionManagerClass={}, dataSourceDetails={}",
                className(jdbcTemplate.getDataSource()),
                identity(jdbcTemplate.getDataSource()),
                className(dataSource),
                identity(dataSource),
                transactionManager.getClass().getName(),
                describeDataSource(dataSource)
        );
    }

    private String describeConnection(Connection connection) {
        try {
            DatabaseMetaData metaData = connection.getMetaData();
            return "connectionClass=" + className(connection) +
                    ", connectionId=" + identity(connection) +
                    ", readOnly=" + connection.isReadOnly() +
                    ", autoCommit=" + connection.getAutoCommit() +
                    ", jdbcUrl=" + metaData.getURL();
        } catch (Exception ex) {
            return "connectionClass=" + className(connection) +
                    ", connectionId=" + identity(connection) +
                    ", describeError=" + ex.getMessage();
        }
    }

    private String describeDataSource(DataSource target) {
        if (target instanceof HikariDataSource hikari) {
            return "poolName=" + hikari.getPoolName() +
                    ", jdbcUrl=" + hikari.getJdbcUrl() +
                    ", hikariReadOnly=" + hikari.isReadOnly();
        }
        return "type=" + className(target);
    }

    private String className(Object target) {
        return target == null ? "null" : target.getClass().getName();
    }

    private String identity(Object target) {
        return target == null ? "null" : Integer.toHexString(System.identityHashCode(target));
    }
}
