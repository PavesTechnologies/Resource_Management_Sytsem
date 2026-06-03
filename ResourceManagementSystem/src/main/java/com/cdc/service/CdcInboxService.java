package com.cdc.service;

import com.cdc.payload.CdcEventPayload;
import com.cdc.payload.CdcPayloadCodec;
import com.entity.ledger_entities.LedgerEventLog;
import com.entity_enums.ledger_enums.EventStatus;
import com.repo.ledger_repo.LedgerEventLogRepository;
import io.debezium.engine.RecordChangeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.connect.source.SourceRecord;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CdcInboxService {

    private final LedgerEventLogRepository ledgerEventLogRepository;
    private final CdcPayloadCodec cdcPayloadCodec;

    @Transactional
    public LedgerEventLog persist(String connectorName,
                                  String tableName,
                                  String operation,
                                  String entityType,
                                  String entityId,
                                  RecordChangeEvent<SourceRecord> event) {
        CdcEventPayload payload = cdcPayloadCodec.fromEvent(
                connectorName, tableName, operation, entityType, entityId, event);
        String eventId = cdcPayloadCodec.computeEventId(connectorName, event, payload);
        String eventHash = cdcPayloadCodec.computeEventHash(payload);

        return ledgerEventLogRepository.findByEventId(eventId)
                .orElseGet(() -> createInboxEntry(eventId, eventHash, payload));
    }

    private LedgerEventLog createInboxEntry(String eventId, String eventHash, CdcEventPayload payload) {
        LedgerEventLog eventLog = LedgerEventLog.builder()
                .eventId(eventId)
                .resourceId(payload.getEntityId())
                .eventType("CDC")
                .eventHash(eventHash)
                .connectorName(payload.getConnectorName())
                .entityType(payload.getEntityType())
                .entityId(payload.getEntityId())
                .sourceTable(payload.getTableName())
                .operationType(payload.getOperation())
                .eventSource("CDC")
                .payload(cdcPayloadCodec.serialize(payload))
                .sourceTimestamp(payload.getSourceTimestamp())
                .processedFlag(false)
                .retryCount(0)
                .status(EventStatus.NEW)
                .build();
        try {
            return ledgerEventLogRepository.saveAndFlush(eventLog);
        } catch (DataIntegrityViolationException ex) {
            log.info("CDC inbox event already persisted for entityType={}, entityId={}, eventId={}",
                    payload.getEntityType(), payload.getEntityId(), eventId);
            return ledgerEventLogRepository.findByEventId(eventId)
                    .orElseThrow(() -> ex);
        }
    }
}
