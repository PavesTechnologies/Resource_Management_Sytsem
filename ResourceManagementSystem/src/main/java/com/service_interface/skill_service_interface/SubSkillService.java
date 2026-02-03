package com.service_interface.skill_service_interface;

import com.entity.skill_entities.SubSkill;
import com.dto.UserDTO;

import java.util.List;
import java.util.UUID;

public interface SubSkillService {

    SubSkill createSubSkill(
            UUID skillId,
            String subSkillName,
            String description,
            boolean isCertification,
            String proficiencyName,
            UserDTO user
    );

    void deactivateSubSkill(UUID subSkillId, UserDTO user);

    List<SubSkill> getActiveSubSkills(UUID skillId);
}
