package com.service_interface.skill_service_interface;

import com.entity.skill_entities.Skill;
import com.dto.UserDTO;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SkillService {

    Skill createSkill(
            UUID categoryId,
            String skillName,
            String description,
            boolean isCertification,
            String proficiencyName,
            UserDTO user
    );

    Skill updateSkill(
            UUID skillId,
            String skillName,
            String description,
            String proficiencyName,
            UserDTO user
    );

    void deactivateSkill(UUID skillId, UserDTO user);

    Optional<Skill> getActiveSkill(UUID skillId);

    List<Skill> getActiveSkillsByCategory(UUID categoryId);
}
