package com.cdc.listener;

import com.cdc.config.EosTrackedColumns;
import com.cdc.execution.CdcSafeExecutor;
import com.cdc.protection.StaleEventProtectionService;
import com.cdc.service.EosResourceSyncService;
import com.cdc.throttling.ReplayThrottlingService;
import com.cdc.util.DebeziumChangeDetector;
import com.cdc.validation.ReplayFreshnessValidationService;
import com.entity.resource_entities.Resource;
import com.repo.resource_repo.ResourceRepository;
import io.debezium.engine.RecordChangeEvent;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.source.SourceRecord;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class EosCdcHandler {

    private final EosResourceSyncService eosResourceSyncService;
    private final CdcSafeExecutor cdcSafeExecutor;
    private final StaleEventProtectionService staleEventProtectionService;
    private final ReplayThrottlingService replayThrottlingService;
    private final ReplayFreshnessValidationService replayFreshnessValidationService;
    private final ResourceRepository resourceRepository;

    @PostConstruct
    void initFreshnessThresholds() {
        replayFreshnessValidationService.configureFreshnessThreshold("EOS-employee_details", 30);
        replayFreshnessValidationService.configureFreshnessThreshold("EOS-offer_letter_details", 60);
        replayFreshnessValidationService.configureFreshnessThreshold("EOS-employee_exit", 60);
    }

    public void handleEvent(RecordChangeEvent<SourceRecord> event) {

        Struct value = (Struct) event.record().value();
        if (value == null) return;

        String operation = value.getString("op");
        Struct before = value.getStruct("before");
        Struct after  = value.getStruct("after");
        String tableName = extractTableName(event);

        if (!isSupportedTable(tableName)) return;

        String entityType = "EOS-" + tableName;
        String entityId   = extractEntityId(after != null ? after : before);
        String opLabel    = "d".equals(operation) ? "DELETE"
                          : "c".equals(operation) || "r".equals(operation) ? "CREATE"
                          : "UPDATE";

        // --- Throttle check ---
        ReplayThrottlingService.ThrottlingResult throttle =
                replayThrottlingService.checkReplayAllowed(entityType);
        if (!throttle.isAllowed()) {
            log.warn("EOS CDC throttled - entityType={}, entityId={}, reason={}",
                    entityType, entityId, throttle.getReason());
            return;
        }

        // --- Stale-event + freshness check (UPDATE / INSERT only) ---
        if (after != null && !"d".equals(operation)) {

            // Skip processing if no tracked columns changed (UPDATE path only)
            if (before != null) {
                Set<String> changed = DebeziumChangeDetector.detectChangedColumns(before, after);
                if (!EosTrackedColumns.containsTrackedChanges(changed)) {
                    log.debug("EOS CDC skipped - no tracked columns changed for entityId={}", entityId);
                    return;
                }
            }

            LocalDateTime incomingTs = staleEventProtectionService.extractEventTimestamp(after);
            if (incomingTs != null) {
                Resource existing = resourceRepository.findById(entityId).orElse(null);

                // Stale-event guard
                if (existing != null && staleEventProtectionService.isStaleEvent(
                        existing.getChangedAt(), incomingTs, entityId)) {
                    log.warn("EOS CDC stale event rejected - entityId={}", entityId);
                    return;
                }

                // Freshness validation (warn-only; does not block processing)
                ReplayFreshnessValidationService.FreshnessValidationResult freshness =
                        replayFreshnessValidationService.validateFreshness(entityType, incomingTs, entityId);
                if (!freshness.isValid()) {
                    log.warn("EOS CDC freshness check failed - entityId={}, message={}",
                            entityId, freshness.getMessage());
                }
            }
        }

        log.info("EOS CDC EVENT -> table={}, op={}, entityId={}", tableName, operation, entityId);

        long startTime = System.currentTimeMillis();

        cdcSafeExecutor.execute(entityType, entityId, opLabel, event.record().toString(), () -> {
            try {
                dispatchEvent(tableName, operation, before, after);
            } finally {
                replayThrottlingService.recordReplayCompletion(
                        entityType, System.currentTimeMillis() - startTime);
            }
        });
    }

    // -------------------------------------------------------------------------

    private void dispatchEvent(String tableName, String operation, Struct before, Struct after) {
        if ("d".equals(operation)) {
            eosResourceSyncService.handleDelete(tableName, before);
            return;
        }
        if (after == null) return;
        switch (tableName) {
            case "employee_details"     -> eosResourceSyncService.processEmployeeDetails(after);
            case "offer_letter_details" -> eosResourceSyncService.processOfferDetails(after);
            case "employee_exit"        -> eosResourceSyncService.processEmployeeExit(after);
        }
    }

    private boolean isSupportedTable(String tableName) {
        return "employee_details".equals(tableName)
                || "offer_letter_details".equals(tableName)
                || "employee_exit".equals(tableName);
    }

    private String extractTableName(RecordChangeEvent<SourceRecord> event) {
        Struct value = (Struct) event.record().value();
        if (value == null) return "unknown";
        Struct source = value.getStruct("source");
        return source != null ? source.getString("table") : "unknown";
    }

    private String extractEntityId(Struct struct) {
        if (struct == null) return "unknown";
        // employee_details and employee_exit use employee_id; offer_letter_details uses mail
        if (struct.schema().field("employee_id") != null) {
            Object id = struct.get("employee_id");
            if (id != null) return id.toString();
        }
        if (struct.schema().field("mail") != null) {
            Object mail = struct.get("mail");
            if (mail != null) return mail.toString();
        }
        return "unknown";
    }
}
