package com.service_interface.skill_service_interface;


public interface SkillAuditService {

    void auditCreate(
            String entityName,
            String entityId,
            Object newValue,
            String changedBy
    );

    void auditUpdate(
            String entityName,
            String entityId,
            Object oldValue,
            Object newValue,
            String changedBy
    );

    void auditDelete(
            String entityName,
            String entityId,
            Object oldValue,
            String changedBy
    );
}
