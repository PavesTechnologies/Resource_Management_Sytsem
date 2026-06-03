package com.cdc.service;

import com.cdc.runner.CdcLeadershipLifecycle;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.Nullable;

/**
 * Generic, leadership-scoped JDBC connection pool for CDC source databases.
 *
 * The pool is created only when this instance wins CDC leadership and is
 * destroyed immediately when leadership is lost or the instance shuts down.
 * Non-leader instances hold zero connections to the source database.
 *
 * Use one instance per source database (EOS, PMS, …) and register it as the
 * CdcLeadershipLifecycle on the corresponding UnifiedDebeziumRunner.
 */
@Slf4j
public class CdcConnectionManager implements CdcLeadershipLifecycle {

    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final String poolName;
    private final boolean readOnly;

    private volatile HikariDataSource dataSource;
    private volatile JdbcTemplate template;

    public CdcConnectionManager(String jdbcUrl,
                                 String username,
                                 String password,
                                 String poolName,
                                 boolean readOnly) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
        this.poolName = poolName;
        this.readOnly = readOnly;
    }

    @Override
    public synchronized void onLeadershipAcquired() {
        if (dataSource != null && !dataSource.isClosed()) {
            return;
        }
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setMaximumPoolSize(2);
        config.setMinimumIdle(0);
        config.setConnectionTimeout(30_000);
        config.setReadOnly(readOnly);
        config.setPoolName(poolName);
        dataSource = new HikariDataSource(config);
        template = new JdbcTemplate(dataSource);
        log.info("[{}] CDC connection pool opened — this instance is the leader", poolName);
    }

    @Override
    public synchronized void onLeadershipLost() {
        if (dataSource != null) {
            dataSource.close();
            dataSource = null;
            template = null;
            log.info("[{}] CDC connection pool closed — leadership released", poolName);
        }
    }

    public boolean isOpen() {
        return template != null;
    }

    /**
     * Returns the JdbcTemplate for this source database.
     * Only call from code paths that execute under CDC leadership.
     */
    public JdbcTemplate getTemplate() {
        JdbcTemplate t = this.template;
        if (t == null) {
            throw new IllegalStateException(
                    "[" + poolName + "] Connection pool is not available — this instance is not the CDC leader");
        }
        return t;
    }

    // ── optional convenience: expose pool name for logging ──────────────────
    @Nullable
    public String getPoolName() {
        return poolName;
    }
}
