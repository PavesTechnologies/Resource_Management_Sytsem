package com.cdc.service;

import com.cdc.model.CdcProcessingOutcome;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class EosDirectResyncService {

    private final JdbcTemplate eosJdbcTemplate;
    private final EosResourceSyncService eosResourceSyncService;

    public EosDirectResyncService(
            @Qualifier("eosJdbcTemplate") JdbcTemplate eosJdbcTemplate,
            EosResourceSyncService eosResourceSyncService) {
        this.eosJdbcTemplate = eosJdbcTemplate;
        this.eosResourceSyncService = eosResourceSyncService;
    }

    public boolean resync(String entityType, String entityId) {
        return switch (entityType) {
            case "EOS-employee_details" -> resyncEmployeeDetails(entityId);
            case "EOS-offer_letter_details" -> resyncOfferLetterDetails(entityId);
            case "EOS-employee_exit" -> resyncEmployeeExit(entityId);
            default -> {
                log.warn("Unknown EOS entity type for resync: {}", entityType);
                yield false;
            }
        };
    }

    private boolean resyncEmployeeDetails(String employeeId) {
        Map<String, Object> row = fetchOne(
                "SELECT * FROM employee_details WHERE employee_id = ?", employeeId);
        if (row == null) {
            log.warn("EOS re-sync: employee_details not found for employeeId={}", employeeId);
            return false;
        }
        eosResourceSyncService.processEmployeeDetailsFromMap(row);
        return true;
    }

    private boolean resyncOfferLetterDetails(String entityId) {
        // offer_letter_details links to employee_details via user_uuid (no direct employee_id column).
        // JOIN fetches both the offer fields and the employee_id in one query.
        // Retry path: entityId = personal mail; Admin path: entityId = employee_id.
        String joinSql;
        if (entityId.contains("@")) {
            joinSql = "SELECT o.*, e.employee_id AS employee_id"
                    + " FROM offer_letter_details o"
                    + " JOIN employee_details e ON e.user_uuid = o.user_uuid"
                    + " WHERE o.mail = ?";
        } else {
            joinSql = "SELECT o.*, e.employee_id AS employee_id"
                    + " FROM offer_letter_details o"
                    + " JOIN employee_details e ON e.user_uuid = o.user_uuid"
                    + " WHERE e.employee_id = ?";
        }

        Map<String, Object> row = fetchOne(joinSql, entityId);
        if (row == null) {
            log.warn("EOS re-sync: offer_letter_details not found for entityId={}", entityId);
            return false;
        }

        log.info("EOS re-sync: offer_letter resolved for entityId={}, employee_id={}",
                entityId, row.get("employee_id"));
        CdcProcessingOutcome outcome = eosResourceSyncService.processOfferDetailsFromMap(row, null, false);
        if (outcome.getOutcomeType() == CdcProcessingOutcome.OutcomeType.WAITING_FOR_DEPENDENCY) {
            log.info("EOS re-sync left offer_letter_details waiting for dependency: entityId={}, reason={}, lifecycleStatus={}",
                    entityId, outcome.getReasonCode(), outcome.getLifecycleStatus());
            return false;
        }
        return true;
    }

    private boolean resyncEmployeeExit(String employeeId) {
        Map<String, Object> row = fetchOne(
                "SELECT * FROM employee_exit WHERE employee_id = ?", employeeId);
        if (row == null) {
            log.warn("EOS re-sync: employee_exit not found for employeeId={}", employeeId);
            return false;
        }
        eosResourceSyncService.processEmployeeExitFromMap(row);
        return true;
    }

    private Map<String, Object> fetchOne(String sql, Object param) {
        try {
            return eosJdbcTemplate.queryForMap(sql, param);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }
}
