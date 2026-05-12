package com.cdc.service;

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

    public void resync(String entityType, String entityId) {
        switch (entityType) {
            case "EOS-employee_details"     -> resyncEmployeeDetails(entityId);
            case "EOS-offer_letter_details" -> resyncOfferLetterDetails(entityId);
            case "EOS-employee_exit"        -> resyncEmployeeExit(entityId);
            default -> log.warn("Unknown EOS entity type for resync: {}", entityType);
        }
    }

    private void resyncEmployeeDetails(String employeeId) {
        Map<String, Object> row = fetchOne(
                "SELECT * FROM employee_details WHERE employee_id = ?", employeeId);
        if (row == null) {
            log.warn("EOS re-sync: employee_details not found for employeeId={}", employeeId);
            return;
        }
        eosResourceSyncService.processEmployeeDetailsFromMap(row);
    }

    private void resyncOfferLetterDetails(String entityId) {
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
            return;
        }

        log.info("EOS re-sync: offer_letter resolved for entityId={}, employee_id={}",
                entityId, row.get("employee_id"));
        eosResourceSyncService.processOfferDetailsFromMap(row);
    }

    private void resyncEmployeeExit(String employeeId) {
        Map<String, Object> row = fetchOne(
                "SELECT * FROM employee_exit WHERE employee_id = ?", employeeId);
        if (row == null) {
            log.warn("EOS re-sync: employee_exit not found for employeeId={}", employeeId);
            return;
        }
        eosResourceSyncService.processEmployeeExitFromMap(row);
    }

    private Map<String, Object> fetchOne(String sql, Object param) {
        try {
            return eosJdbcTemplate.queryForMap(sql, param);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }
}
