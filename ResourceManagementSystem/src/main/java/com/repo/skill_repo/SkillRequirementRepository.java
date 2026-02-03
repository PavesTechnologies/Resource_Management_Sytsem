package com.repo.skill_repo;

import com.entity.skill_entities.SkillRequirement;
import com.entity_enums.skill_enums.AppliesToType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SkillRequirementRepository extends JpaRepository<SkillRequirement, UUID> {

    List<SkillRequirement> findByAppliesToTypeAndAppliesToId(
            AppliesToType appliesToType,
            Long appliesToId
    );
}
