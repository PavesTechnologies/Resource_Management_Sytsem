package com.service_interface.skill_service_interface;

import com.dto.skill_taxonomy.SkillTaxonomyTreeDto;
import com.entity.skill_entities.SkillCategory;

import java.util.List;
import java.util.UUID;

public interface SkillCategoryService {

    SkillCategory create(String name, String description);

    List<SkillCategory> findAll();

    List<SkillCategory> findActiveCategories();

    void deactivateCategory(UUID categoryId);

    List<SkillTaxonomyTreeDto> getSkillTaxonomyTree();

    SkillTaxonomyTreeDto getSkillTaxonomyTreeByCategoryId(UUID categoryId);
}

