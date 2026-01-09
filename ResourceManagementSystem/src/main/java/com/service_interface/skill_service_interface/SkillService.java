package com.service_interface.skill_service_interface;

import com.entity.skill_entities.Skill;
import com.dto.UserDTO;

import java.util.List;
import java.util.Optional;

public interface SkillService {

    Skill createSkill(
            Long categoryId,
            String skillName,
            String description,
            boolean isCertification,
            String proficiencyName,
            UserDTO user
    );

    Skill updateSkill(
            Long skillId,
            String skillName,
            String description,
            String proficiencyName,
            UserDTO user
    );

    void deactivateSkill(Long skillId, UserDTO user);

    Optional<Skill> getActiveSkill(Long skillId);

    List<Skill> getActiveSkillsByCategory(Long categoryId);
}
