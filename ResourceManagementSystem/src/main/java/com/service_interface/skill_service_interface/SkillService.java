package com.service_interface.skill_service_interface;

import com.entity.skill_entities.Skill;

import java.util.List;
import java.util.UUID;

public interface SkillService {

    Skill create(UUID categoryId, String name, String description);

    List<Skill> findActiveSkills();

    List<Skill> findActiveSkillsByCategoryId(UUID categoryId);

    void deactivateSkill(UUID skillId);

    Skill update(UUID skillId, UUID categoryId, String name, String description);
}

