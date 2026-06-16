package com.cdc.listener;

import com.cdc.config.EosTrackedColumns;
import com.cdc.model.CdcProcessingOutcome;
import com.cdc.payload.CdcEventPayload;
import com.cdc.service.CdcInboxService;
import com.cdc.service.EosResourceSyncService;
import com.entity.ledger_entities.LedgerEventLog;
import com.entity_enums.ledger_enums.EventStatus;
import com.cdc.throttling.ReplayThrottlingService;
import com.cdc.validation.ReplayFreshnessValidationService;
import io.debezium.engine.RecordChangeEvent;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.source.SourceRecord;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class EosCdcHandler {

    private final EosResourceSyncService eosResourceSyncService;
    private final ReplayThrottlingService replayThrottlingService;
    private final ReplayFreshnessValidationService replayFreshnessValidationService;
    private final CdcInboxService cdcInboxService;

    @PostConstruct
    void initFreshnessThresholds() {
        replayFreshnessValidationService.configureFreshnessThreshold("EOS-employee_details", 30);
        replayFreshnessValidationService.configureFreshnessThreshold("EOS-offer_letter_details", 60);
        replayFreshnessValidationService.configureFreshnessThreshold("EOS-employee_exit", 60);
    }

    public void handleEvent(RecordChangeEvent<SourceRecord> event) {
        Struct value = (Struct) event.record().value();
        if (value == null) {
            return;
        }

        String operation = value.getString("op");
        Struct before = value.getStruct("before");
        Struct after = value.getStruct("after");
        String tableName = extractTableName(event);
        if (!isSupportedTable(tableName)) {
            return;
        }

        String entityType = "EOS-" + tableName;
        String entityId = extractEntityId(after != null ? after : before);
        ReplayThrottlingService.ThrottlingResult throttle = replayThrottlingService.checkReplayAllowed(entityType);
        if (!throttle.isAllowed()) {
            log.warn("EOS CDC throttled - entityType={}, entityId={}, reason={}",
                    entityType, entityId, throttle.getReason());
            return;
        }

        cdcInboxService.persist("EOS", tableName, operation, entityType, entityId, event);
    }

    @Transactional
    public CdcProcessingOutcome processInboxEvent(CdcEventPayload payload) {
        return processInboxEvent(payload, null);
    }

    @Transactional
    public CdcProcessingOutcome processInboxEvent(CdcEventPayload payload, LedgerEventLog eventLog) {
        long startTime = System.currentTimeMillis();
        try {
            if (payload.getSourceTimestamp() != null) {
                ReplayFreshnessValidationService.FreshnessValidationResult freshness =
                        replayFreshnessValidationService.validateFreshness(
                                payload.getEntityType(), payload.getSourceTimestamp().atOffset(java.time.ZoneOffset.UTC).toLocalDateTime(), payload.getEntityId());
                if (!freshness.isValid()) {
                    log.warn("EOS CDC freshness check failed - entityId={}, message={}",
                            payload.getEntityId(), freshness.getMessage());
                }
            }

            if ("d".equals(payload.getOperation())) {
                eosResourceSyncService.handleDeleteFromMap(payload.getTableName(), payload.getBefore(), payload.getSourceTimestamp());
                return CdcProcessingOutcome.success();
            }

            if (payload.getAfter() == null) {
                return CdcProcessingOutcome.success();
            }

            if (isTrackedUpdate(payload)) {
                boolean dependencyReplay = eventLog != null
                        && eventLog.getStatus() == EventStatus.WAITING_FOR_DEPENDENCY;
                switch (payload.getTableName()) {
                    case "employee_details" -> {
                        eosResourceSyncService.processEmployeeDetailsFromMap(payload.getAfter(), payload.getSourceTimestamp());
                        return CdcProcessingOutcome.success();
                    }
                    case "offer_letter_details" -> {
                        return eosResourceSyncService.processOfferDetailsFromMap(
                                payload.getAfter(), payload.getSourceTimestamp(), dependencyReplay);
                    }
                    case "employee_exit" -> {
                        eosResourceSyncService.processEmployeeExitFromMap(payload.getAfter(), payload.getSourceTimestamp());
                        return CdcProcessingOutcome.success();
                    }
                    default -> {
                    }
                }
            }
            return CdcProcessingOutcome.success();
        } finally {
            replayThrottlingService.recordReplayCompletion(
                    payload.getEntityType(),
                    System.currentTimeMillis() - startTime
            );
        }
    }

    private boolean isTrackedUpdate(CdcEventPayload payload) {
        if (payload.getBefore() == null || payload.getAfter() == null) {
            return true;
        }
        Set<String> changed = new LinkedHashSet<>();
        payload.getAfter().forEach((key, value) -> {
            if (!Objects.equals(value, payload.getBefore().get(key))) {
                changed.add(key);
            }
        });
        return EosTrackedColumns.containsTrackedChanges(changed);
    }

    private boolean isSupportedTable(String tableName) {
        return "employee_details".equals(tableName)
                || "offer_letter_details".equals(tableName)
                || "employee_exit".equals(tableName);
    }

    private String extractTableName(RecordChangeEvent<SourceRecord> event) {
        Struct value = (Struct) event.record().value();
        if (value == null) {
            return "unknown";
        }
        Struct source = value.getStruct("source");
        return source != null ? source.getString("table") : "unknown";
    }

    private String extractEntityId(Struct struct) {
        if (struct == null) {
            return "unknown";
        }
        // employee_details and employee_exit have employee_id — most stable key
        if (struct.schema().field("employee_id") != null) {
            Object id = struct.get("employee_id");
            if (id != null) {
                return id.toString();
            }
        }
        // offer_letter_details has no employee_id; user_uuid is the reliable join key
        // (same value as employee_details.user_uuid — do NOT use mail which is personal email)
        if (struct.schema().field("user_uuid") != null) {
            Object uuid = struct.get("user_uuid");
            if (uuid != null) {
                return uuid.toString();
            }
        }
        if (struct.schema().field("mail") != null) {
            Object mail = struct.get("mail");
            if (mail != null) {
                return mail.toString();
            }
        }
        return "unknown";
    }
}
