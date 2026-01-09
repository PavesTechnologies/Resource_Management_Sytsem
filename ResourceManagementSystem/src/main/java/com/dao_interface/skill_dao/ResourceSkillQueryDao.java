package com.dao_interface.skill_dao;

import com.entity.skill_entities.ResourceSkill;

import java.util.List;
import java.util.Optional;

public interface ResourceSkillQueryDao {

    Optional<ResourceSkill> findActiveSkillForResource(
            Long resourceId,
            Long skillId
    );

    List<ResourceSkill> findAllActiveSkillsForResource(Long resourceId);
}
