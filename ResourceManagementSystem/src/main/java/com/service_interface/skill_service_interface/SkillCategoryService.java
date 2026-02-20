package com.service_interface.skill_service_interface;

import com.dto.skill_dto.SkillSearchResultDto;
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

    /**
     * OPTIMIZED: Search skills across categories, skills, and subskills.
     * Uses DTO projections to avoid cartesian products and reduce memory usage.
     * 
     * @param searchTerm Partial name to search for (case-insensitive)
     * @return List of unified search results with hierarchical context
     */
    List<SkillSearchResultDto> searchSkills(String searchTerm);
}

