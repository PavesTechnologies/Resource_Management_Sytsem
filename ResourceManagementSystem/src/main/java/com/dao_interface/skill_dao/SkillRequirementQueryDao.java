package com.dao_interface.skill_dao;

import com.entity.skill_entities.SkillRequirement;
import com.entity_enums.skill_enums.AppliesToType;

import java.util.List;

public interface SkillRequirementQueryDao {

    List<SkillRequirement> findRequirements(
            AppliesToType appliesToType,
            Long appliesToId
    );
}
