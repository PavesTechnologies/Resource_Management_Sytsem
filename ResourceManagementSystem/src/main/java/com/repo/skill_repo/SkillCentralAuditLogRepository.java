package com.repo.skill_repo;

import com.entity.skill_entities.SkillCentralAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SkillCentralAuditLogRepository
        extends JpaRepository<SkillCentralAuditLog, UUID> {
}
