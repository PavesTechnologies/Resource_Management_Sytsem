package com.cdc.retry;

import com.cdc.service.EosDirectResyncService;
import com.entity.ledger_entities.LedgerEventLog;
import com.entity_enums.ledger_enums.EventStatus;
import com.events.handler.LedgerEventHandler;
import com.repo.ledger_repo.LedgerEventLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UnifiedCdcRetryService {

    private final LedgerEventHandler ledgerEventHandler;
    private final LedgerEventLogRepository ledgerEventLogRepository;
    private final EosDirectResyncService eosDirectResyncService;

    @Transactional
    public void processFailedCdcEvents() {
        int processed = ledgerEventHandler.processPendingCdcEvents(50);
        int recovered = ledgerEventHandler.recoverStalledCdcEvents();
        log.info("CDC retry cycle completed: processed={}, recovered={}", processed, recovered);
    }

    @Transactional
    public void processCdcDlqEntries() {
        List<LedgerEventLog> deadLetters = ledgerEventLogRepository.findByConnectorNameAndStatusIn(
                "EOS",
                List.of(EventStatus.DEAD_LETTER)
        );
        for (LedgerEventLog event : deadLetters) {
            try {
                eosDirectResyncService.resync(event.getEntityType(), event.getEntityId());
                event.setStatus(EventStatus.CANCELLED);
                event.setErrorMessage(null);
                event.setNextRetryAt(null);
                event.setUpdatedAt(LocalDateTime.now());
                ledgerEventLogRepository.save(event);
            } catch (Exception ex) {
                log.error("EOS direct resync failed for eventId={}: {}", event.getEventId(), ex.getMessage(), ex);
            }
        }
    }

    @Transactional
    public void cleanupOldCdcFailures() {
        List<LedgerEventLog> legacyFailures = ledgerEventLogRepository.findByStatus(EventStatus.PERMANENTLY_FAILED);
        if (!legacyFailures.isEmpty()) {
            log.info("Legacy CDC failure rows still present: {}", legacyFailures.size());
        }
    }
}
