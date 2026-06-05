package com.repo.skill_repo;

import com.entity.skill_entities.SkillRequirement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SkillRequirementRepository extends JpaRepository<SkillRequirement, UUID> {

    List<SkillRequirement> findByAppliesToIdAndActiveFlagTrue(Long projectId);
}
