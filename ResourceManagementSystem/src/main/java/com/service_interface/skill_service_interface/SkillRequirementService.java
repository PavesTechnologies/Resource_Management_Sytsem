package com.service_interface.skill_service_interface;

import com.entity.skill_entities.SkillRequirement;
import com.entity_enums.skill_enums.AppliesToType;
import com.dto.UserDTO;

import java.util.List;

public interface SkillRequirementService {

    SkillRequirement addRequirement(
            AppliesToType appliesToType,
            Long appliesToId,
            Long skillId,
            String proficiencyName,
            Boolean mandatory,
            UserDTO user
    );

    List<SkillRequirement> getRequirements(
            AppliesToType appliesToType,
            Long appliesToId
    );
}
