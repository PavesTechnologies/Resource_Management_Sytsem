package com.service_imple.skill_service_impl;

import com.entity.skill_entities.SkillCentralAuditLog;
import com.repo.skill_repo.SkillCentralAuditLogRepository;
import com.service_interface.skill_service_interface.SkillAuditService;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SkillAuditServiceImpl implements SkillAuditService {

    private final SkillCentralAuditLogRepository auditRepo;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void auditCreate(
            String entityName,
            String entityId,
            Object newValue,
            String changedBy) {

        saveAudit(entityName, entityId, null, newValue, "CREATE", changedBy);
    }

    @Override
    public void auditUpdate(
            String entityName,
            String entityId,
            Object oldValue,
            Object newValue,
            String changedBy) {

        saveAudit(entityName, entityId, oldValue, newValue, "UPDATE", changedBy);
    }

    @Override
    public void auditDelete(
            String entityName,
            String entityId,
            Object oldValue,
            String changedBy) {

        saveAudit(entityName, entityId, oldValue, null, "DELETE", changedBy);
    }

    private void saveAudit(
            String entityName,
            String entityId,
            Object oldValue,
            Object newValue,
            String action,
            String changedBy) {

        try {
            SkillCentralAuditLog log = SkillCentralAuditLog.builder()
                    .entityName(entityName)
                    .entityId(entityId)
                    .oldValue(
                            oldValue == null ? null : objectMapper.writeValueAsString(oldValue)
                    )
                    .newValue(
                            newValue == null ? null : objectMapper.writeValueAsString(newValue)
                    )
                    .operation(action)
                    .changedBy(changedBy)
                    .changedAt(LocalDateTime.now())
                    .build();

            auditRepo.save(log);

        } catch (Exception e) {
            throw new IllegalStateException("Failed to write audit log", e);
        }
    }
}
