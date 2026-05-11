package com.cdc.config;

import com.cdc.listener.EosCdcHandler;
import com.cdc.listener.PmsCdcHandler;
import com.cdc.runner.UnifiedDebeziumRunner;
import io.debezium.config.Configuration;
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
public class UnifiedCdcRunnerConfig {

    /**
     * PMS CDC Runner bean.
     * Uses PMS-specific configuration and handler.
     */
    @Bean("pmsDebeziumRunner")
    public UnifiedDebeziumRunner pmsDebeziumRunner(
            Configuration debeziumConfiguration,
            PmsCdcHandler pmsCdcHandler) {
        
        return new UnifiedDebeziumRunner(
                debeziumConfiguration,
                pmsCdcHandler::handleEvent,
                "PMS"
        );
    }

    /**
     * EOS CDC Runner bean.
     * Uses EOS-specific configuration and handler.
     */
    @Bean("eosDebeziumRunner")
    public UnifiedDebeziumRunner eosDebeziumRunner(
            @Qualifier("eosDebeziumConfiguration") Configuration eosDebeziumConfiguration,
            EosCdcHandler eosCdcHandler) {
        
        return new UnifiedDebeziumRunner(
                eosDebeziumConfiguration,
                eosCdcHandler::handleEvent,
                "EOS"
        );
    }
}
