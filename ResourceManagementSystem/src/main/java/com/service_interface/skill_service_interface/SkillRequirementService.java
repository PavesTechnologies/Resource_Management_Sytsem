package com.service_interface.skill_service_interface;

import com.entity.skill_entities.SkillRequirement;
import com.entity_enums.skill_enums.AppliesToType;
import com.dto.UserDTO;

import java.util.List;
import java.util.UUID;

public interface SkillRequirementService {

    SkillRequirement addRequirement(
            AppliesToType appliesToType,
            Long appliesToId,
            UUID skillId,
            String proficiencyName,
            Boolean mandatory,
            UserDTO user
    );

    List<SkillRequirement> getRequirements(
            AppliesToType appliesToType,
            Long appliesToId
    );
}
