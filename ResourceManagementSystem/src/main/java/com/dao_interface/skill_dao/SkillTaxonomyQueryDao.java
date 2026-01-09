package com.dao_interface.skill_dao;

import com.entity.skill_entities.Skill;
import com.entity.skill_entities.SkillCategory;
import com.entity.skill_entities.SubSkill;

import java.util.List;
import java.util.Optional;

public interface SkillTaxonomyQueryDao {

    Optional<SkillCategory> findActiveCategoryByName(String categoryName);

    Optional<Skill> findActiveSkillByNameAndCategory(
            String skillName,
            Long categoryId
    );

    List<Skill> findActiveSkillsByCategory(Long categoryId);

    List<SubSkill> findActiveSubSkillsBySkill(Long skillId);
}
