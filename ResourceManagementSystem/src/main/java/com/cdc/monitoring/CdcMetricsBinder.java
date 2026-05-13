package com.cdc.monitoring;

import com.cdc.service.CdcConnectorLeadershipService;
import com.entity_enums.ledger_enums.DLQStatus;
import com.entity_enums.ledger_enums.EventStatus;
import com.repo.ledger_repo.DeadLetterQueueRepository;
import com.repo.ledger_repo.LedgerEventLogRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.stereotype.Component;

@Component
public class CdcMetricsBinder implements MeterBinder {

    private final LedgerEventLogRepository ledgerEventLogRepository;
    private final DeadLetterQueueRepository deadLetterQueueRepository;
    private final CdcConnectorLeadershipService leadershipService;

    public CdcMetricsBinder(LedgerEventLogRepository ledgerEventLogRepository,
                            DeadLetterQueueRepository deadLetterQueueRepository,
                            CdcConnectorLeadershipService leadershipService) {
        this.ledgerEventLogRepository = ledgerEventLogRepository;
        this.deadLetterQueueRepository = deadLetterQueueRepository;
        this.leadershipService = leadershipService;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        Gauge.builder("rms.cdc.events.ready", () ->
                        ledgerEventLogRepository.findByStatus(EventStatus.NEW).size()
                                + ledgerEventLogRepository.findByStatus(EventStatus.RETRY_SCHEDULED).size())
                .register(registry);

        Gauge.builder("rms.cdc.events.processing", () ->
                        ledgerEventLogRepository.findByStatus(EventStatus.PROCESSING).size())
                .register(registry);

        Gauge.builder("rms.cdc.events.dead_letter", () ->
                        ledgerEventLogRepository.findByStatus(EventStatus.DEAD_LETTER).size())
                .register(registry);

        Gauge.builder("rms.cdc.dlq.pending", () ->
                        deadLetterQueueRepository.findByStatus(DLQStatus.PENDING_RETRY).size())
                .register(registry);

        Gauge.builder("rms.cdc.leader.local", () ->
                        leadershipService.isLeader("CDC_CONNECTOR_PMS") || leadershipService.isLeader("CDC_CONNECTOR_EOS") ? 1 : 0)
                .register(registry);

        Gauge.builder("rms.cdc.leader.pms.local", () ->
                        leadershipService.isLeader("CDC_CONNECTOR_PMS") ? 1 : 0)
                .register(registry);

        Gauge.builder("rms.cdc.leader.eos.local", () ->
                        leadershipService.isLeader("CDC_CONNECTOR_EOS") ? 1 : 0)
                .register(registry);
    }
}
