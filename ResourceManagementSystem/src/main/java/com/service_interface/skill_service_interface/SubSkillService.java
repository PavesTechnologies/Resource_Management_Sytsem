package com.service_interface.skill_service_interface;

import com.entity.skill_entities.SubSkill;

import java.util.List;
import java.util.UUID;

public interface SubSkillService {

    SubSkill create(UUID skillId, String name, String description);

    List<SubSkill> findActiveSubSkills();

    List<SubSkill> findActiveSubSkillsBySkillId(UUID skillId);

    void deactivateSubSkill(UUID subSkillId);
}

