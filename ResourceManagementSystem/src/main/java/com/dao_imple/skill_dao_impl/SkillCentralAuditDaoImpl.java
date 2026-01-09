package com.dao_imple.skill_dao_impl;

import com.dao_interface.skill_dao.SkillCentralAuditDao;
import com.entity.skill_entities.SkillCentralAuditLog;
import com.repo.skill_repo.SkillCentralAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
@RequiredArgsConstructor
public class SkillCentralAuditDaoImpl implements SkillCentralAuditDao {

    private final SkillCentralAuditLogRepository auditRepo;

    @Override
    public void logCreate(
            String entityName,
            String entityId,
            String newValueJson,
            String changedBy,
            LocalDateTime changedAt
    ) {
        auditRepo.save(
                SkillCentralAuditLog.builder()
                        .entityName(entityName)
                        .entityId(entityId)
                        .operation("CREATE")
                        .newValue(newValueJson)
                        .changedBy(changedBy)
                        .changedAt(changedAt)
                        .build()
        );
    }

    @Override
    public void logUpdate(
            String entityName,
            String entityId,
            String oldValueJson,
            String newValueJson,
            String changedBy,
            LocalDateTime changedAt
    ) {
        auditRepo.save(
                SkillCentralAuditLog.builder()
                        .entityName(entityName)
                        .entityId(entityId)
                        .operation("UPDATE")
                        .oldValue(oldValueJson)
                        .newValue(newValueJson)
                        .changedBy(changedBy)
                        .changedAt(changedAt)
                        .build()
        );
    }

    @Override
    public void logDelete(
            String entityName,
            String entityId,
            String oldValueJson,
            String changedBy,
            LocalDateTime changedAt
    ) {
        auditRepo.save(
                SkillCentralAuditLog.builder()
                        .entityName(entityName)
                        .entityId(entityId)
                        .operation("DELETE")
                        .oldValue(oldValueJson)
                        .changedBy(changedBy)
                        .changedAt(changedAt)
                        .build()
        );
    }
}
