package com.cdc.config;

import com.cdc.config.properties.CdcProperties;
import com.cdc.config.properties.EosCdcProperties;
import com.cdc.listener.EosCdcHandler;
import com.cdc.listener.PmsCdcHandler;
import com.cdc.runner.UnifiedDebeziumRunner;
import com.cdc.service.CdcConnectionManager;
import com.cdc.service.CdcConnectorLeadershipService;
import io.debezium.config.Configuration;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;

@org.springframework.context.annotation.Configuration
@RequiredArgsConstructor
public class UnifiedCdcRunnerConfig {

    private final CdcProperties cdcProperties;

    /**
     * PMS CDC runner.
     * PmsCdcHandler writes to RMS via JPA — no external JDBC pool needed,
     * so connectionManager is null.
     */
    @Bean("pmsDebeziumRunner")
    public UnifiedDebeziumRunner pmsDebeziumRunner(
            Configuration debeziumConfiguration,
            PmsCdcHandler pmsCdcHandler,
            CdcConnectorLeadershipService leadershipService) {

        return new UnifiedDebeziumRunner(
                debeziumConfiguration,
                pmsCdcHandler::handleEvent,
                "PMS",
                cdcProperties.isEnabled(),
                leadershipService,
                null
        );
    }

    /**
     * EOS CDC runner.
     * EosCdcHandler queries EOS DB directly via eosConnectionManager,
     * so the pool lifecycle is tied to this runner's leadership.
     */
    @Bean("eosDebeziumRunner")
    public UnifiedDebeziumRunner eosDebeziumRunner(
            @Qualifier("eosDebeziumConfiguration") Configuration eosDebeziumConfiguration,
            EosCdcHandler eosCdcHandler,
            CdcConnectorLeadershipService leadershipService,
            @Qualifier("eosConnectionManager") CdcConnectionManager eosConnectionManager) {

        return new UnifiedDebeziumRunner(
                eosDebeziumConfiguration,
                eosCdcHandler::handleEvent,
                "EOS",
                cdcProperties.isEnabled(),
                leadershipService,
                eosConnectionManager
        );
    }

    /**
     * Leadership-scoped connection pool for the EOS source database.
     * Created here (not in EosResyncDataSourceConfig) so its lifecycle is
     * driven by the EOS runner's leadership, not by Spring context startup.
     */
    @Bean("eosConnectionManager")
    public CdcConnectionManager eosConnectionManager(EosCdcProperties eosCdcProperties) {
        EosCdcProperties.DatabaseProperties db = eosCdcProperties.getDatabase();
        String jdbcUrl = String.format(
                "jdbc:mysql://%s:%d/%s?useSSL=true&allowPublicKeyRetrieval=true",
                db.getHostname(), db.getPort(), db.getName());
        return new CdcConnectionManager(jdbcUrl, db.getUser(), db.getPassword(),
                "eos-resync-pool", true);
    }
}
