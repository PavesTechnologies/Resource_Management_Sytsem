package com.repo.skill_repo;

import com.entity.skill_entities.SkillCentralAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkillCentralAuditLogRepository
        extends JpaRepository<SkillCentralAuditLog, Long> {
}
