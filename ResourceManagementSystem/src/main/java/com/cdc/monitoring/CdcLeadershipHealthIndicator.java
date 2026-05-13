package com.cdc.monitoring;

import com.cdc.service.CdcConnectorLeadershipService;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class CdcLeadershipHealthIndicator implements HealthIndicator {

    private final CdcConnectorLeadershipService leadershipService;

    public CdcLeadershipHealthIndicator(CdcConnectorLeadershipService leadershipService) {
        this.leadershipService = leadershipService;
    }

    @Override
    public Health health() {
        return Health.up()
                .withDetail("cdc.ownerId", leadershipService.ownerId())
                .withDetail("cdc.pmsLeader", leadershipService.currentLeader("CDC_CONNECTOR_PMS").orElse("none"))
                .withDetail("cdc.eosLeader", leadershipService.currentLeader("CDC_CONNECTOR_EOS").orElse("none"))
                .withDetail("cdc.pmsLocalLeader", leadershipService.isLeader("CDC_CONNECTOR_PMS"))
                .withDetail("cdc.eosLocalLeader", leadershipService.isLeader("CDC_CONNECTOR_EOS"))
                .build();
    }
}
