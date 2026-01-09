package com.dao_interface.skill_dao;

import java.time.LocalDateTime;

public interface SkillCentralAuditDao {

    void logCreate(
            String entityName,
            String entityId,
            String newValueJson,
            String changedBy,
            LocalDateTime changedAt
    );

    void logUpdate(
            String entityName,
            String entityId,
            String oldValueJson,
            String newValueJson,
            String changedBy,
            LocalDateTime changedAt
    );

    void logDelete(
            String entityName,
            String entityId,
            String oldValueJson,
            String changedBy,
            LocalDateTime changedAt
    );
}
