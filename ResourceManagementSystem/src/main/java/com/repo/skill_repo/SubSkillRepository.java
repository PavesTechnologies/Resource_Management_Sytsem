package com.repo.skill_repo;

import com.entity.skill_entities.SubSkill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SubSkillRepository extends JpaRepository<SubSkill, UUID> {

    List<SubSkill> findBySkillSkillIdAndActiveFlagTrue(UUID skillId);
}
