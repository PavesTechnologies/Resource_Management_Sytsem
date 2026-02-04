package com.repo.skill_repo;

import com.entity.skill_entities.SkillValidityRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SkillValidityRuleRepository extends JpaRepository<SkillValidityRule, UUID> {
}
