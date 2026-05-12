package com.cdc.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

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

    private void resyncOfferLetterDetails(String email) {
        Map<String, Object> row = fetchOne(
                "SELECT * FROM offer_letter_details WHERE mail = ?", email);
        if (row == null) {
            log.warn("EOS re-sync: offer_letter_details not found for mail={}", email);
            return;
        }
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
