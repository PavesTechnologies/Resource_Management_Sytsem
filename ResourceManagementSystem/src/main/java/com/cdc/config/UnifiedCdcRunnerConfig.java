package com.cdc.config;

import com.cdc.config.properties.CdcProperties;
import com.cdc.listener.EosCdcHandler;
import com.cdc.listener.PmsCdcHandler;
import com.cdc.runner.UnifiedDebeziumRunner;
import com.cdc.service.CdcConnectorLeadershipService;
import io.debezium.config.Configuration;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;

/**
 * Configuration for Unified CDC Runners.
 * 
 * Creates two instances of UnifiedDebeziumRunner:
 * - One for PMS CDC processing
 * - One for EOS CDC processing
 * 
 * Eliminates duplicate runner code while maintaining separate configurations.
 */
@org.springframework.context.annotation.Configuration
@RequiredArgsConstructor
public class UnifiedCdcRunnerConfig {
    private final CdcProperties cdcProperties;

    /**
     * PMS CDC Runner bean.
     * Uses PMS-specific configuration and handler.
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
                leadershipService
        );
    }

    /**
     * EOS CDC Runner bean.
     * Uses EOS-specific configuration and handler.
     */
    @Bean("eosDebeziumRunner")
    public UnifiedDebeziumRunner eosDebeziumRunner(
            @Qualifier("eosDebeziumConfiguration") Configuration eosDebeziumConfiguration,
            EosCdcHandler eosCdcHandler,
            CdcConnectorLeadershipService leadershipService) {
        
        return new UnifiedDebeziumRunner(
                eosDebeziumConfiguration,
                eosCdcHandler::handleEvent,
                "EOS",
                cdcProperties.isEnabled(),
                leadershipService
        );
    }
}
