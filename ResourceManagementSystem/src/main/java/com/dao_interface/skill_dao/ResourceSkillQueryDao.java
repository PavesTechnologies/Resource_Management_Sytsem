package com.dao_interface.skill_dao;

import com.entity.skill_entities.ResourceSkill;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ResourceSkillQueryDao {

    Optional<ResourceSkill> findActiveSkillForResource(
            Long resourceId,
            UUID skillId
    );

    List<ResourceSkill> findAllActiveSkillsForResource(Long resourceId);
}
