package com.cdc.retry;

import com.cdc.service.EosDirectResyncService;
import com.entity.ledger_entities.LedgerEventLog;
import com.entity_enums.ledger_enums.EventStatus;
import com.events.handler.LedgerEventHandler;
import com.repo.ledger_repo.LedgerEventLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
    @Value("${cdc.offer.waiting.max-days:45}")
    private int offerWaitingMaxDays;
    @Value("${cdc.cleanup.success-retention-days:30}")
    private int successRetentionDays;
    @Value("${cdc.cleanup.cancelled-retention-days:7}")
    private int cancelledRetentionDays;
    @Value("${cdc.cleanup.dead-letter-retention-days:90}")
    private int deadLetterRetentionDays;

    public void processFailedCdcEvents() {
        int processed = ledgerEventHandler.processPendingCdcEvents(50);
        int recovered = ledgerEventHandler.recoverStalledCdcEvents();
        log.info("CDC retry cycle completed: processed={}, recovered={}", processed, recovered);
    }

    public void processCdcDlqEntries() {
        List<LedgerEventLog> deadLetters = ledgerEventLogRepository.findByConnectorNameAndStatusIn(
                "EOS",
                List.of(EventStatus.DEAD_LETTER)
        );
        for (LedgerEventLog event : deadLetters) {
            try {
                boolean replayed = eosDirectResyncService.resync(event.getEntityType(), event.getEntityId());
                if (!replayed) {
                    log.warn("EOS direct resync deferred for dead-letter eventId={} because source dependency is still unavailable",
                            event.getEventId());
                    continue;
                }
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
        LocalDateTime now = LocalDateTime.now();
        List<LedgerEventLog> legacyFailures = ledgerEventLogRepository.findByStatus(EventStatus.PERMANENTLY_FAILED);

        int expiredWaiting = ledgerEventLogRepository.cancelExpiredWaitingDependencyEvents(
                "EOS",
                "offer_letter_details",
                EventStatus.WAITING_FOR_DEPENDENCY,
                EventStatus.CANCELLED,
                now.minusDays(offerWaitingMaxDays),
                "offer_letter_details waiting expired due to DEPENDENCY_TIMEOUT",
                now,
                now
        );
        int deletedSuccess = ledgerEventLogRepository.deleteOldCdcEventsByStatus(
                EventStatus.SUCCESS,
                now.minusDays(successRetentionDays)
        );
        int deletedCancelled = ledgerEventLogRepository.deleteOldCdcEventsByStatus(
                EventStatus.CANCELLED,
                now.minusDays(cancelledRetentionDays)
        );
        int deletedDeadLetter = ledgerEventLogRepository.deleteOldCdcEventsByStatus(
                EventStatus.DEAD_LETTER,
                now.minusDays(deadLetterRetentionDays)
        );

        if (!legacyFailures.isEmpty()) {
            log.info("Legacy CDC failure rows still present: {}", legacyFailures.size());
        }
        log.info("CDC cleanup completed: expiredWaiting={}, deletedSuccess={}, deletedCancelled={}, deletedDeadLetter={}",
                expiredWaiting, deletedSuccess, deletedCancelled, deletedDeadLetter);
    }
}
