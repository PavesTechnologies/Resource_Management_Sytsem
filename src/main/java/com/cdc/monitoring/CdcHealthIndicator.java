package com.cdc.monitoring;

import com.entity_enums.ledger_enums.EventStatus;
import com.repo.ledger_repo.DeadLetterQueueRepository;
import com.repo.ledger_repo.LedgerEventLogRepository;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class CdcHealthIndicator implements HealthIndicator {

    private final LedgerEventLogRepository ledgerEventLogRepository;
    private final DeadLetterQueueRepository deadLetterQueueRepository;

    public CdcHealthIndicator(LedgerEventLogRepository ledgerEventLogRepository,
                              DeadLetterQueueRepository deadLetterQueueRepository) {
        this.ledgerEventLogRepository = ledgerEventLogRepository;
        this.deadLetterQueueRepository = deadLetterQueueRepository;
    }

    @Override
    public Health health() {
        long ready = ledgerEventLogRepository.findByStatus(EventStatus.NEW).size()
                + ledgerEventLogRepository.findByStatus(EventStatus.RETRY_SCHEDULED).size();
        long deadLetters = ledgerEventLogRepository.findByStatus(EventStatus.DEAD_LETTER).size();
        long legacyFailures = ledgerEventLogRepository.findByStatus(EventStatus.PERMANENTLY_FAILED).size();

        Health.Builder builder = deadLetters > 0 ? Health.status("DEGRADED") : Health.up();
        return builder
                .withDetail("cdc.readyEvents", ready)
                .withDetail("cdc.deadLetters", deadLetters)
                .withDetail("cdc.legacyFailures", legacyFailures)
                .withDetail("cdc.dlqEntries", deadLetterQueueRepository.count())
                .build();
    }
}
