package com.service_interface.skill_service_interface;

import com.entity.skill_entities.SubSkill;
import com.dto.UserDTO;

import java.util.List;

public interface SubSkillService {

    SubSkill createSubSkill(
            Long skillId,
            String subSkillName,
            String description,
            boolean isCertification,
            String proficiencyName,
            UserDTO user
    );

    void deactivateSubSkill(Long subSkillId, UserDTO user);

    List<SubSkill> getActiveSubSkills(Long skillId);
}
