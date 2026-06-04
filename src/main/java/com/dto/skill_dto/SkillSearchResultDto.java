package com.dto.skill_dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Unified DTO for skill search results across categories, skills, and subskills.
 * Provides hierarchical context for each match type.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillSearchResultDto {
    
    /**
     * Entity type: CATEGORY, SKILL, or SUBSKILL
     */
    private String type;
    
    /**
     * Unique identifier of the entity
     */
    private UUID id;
    
    /**
     * Name of the entity (category, skill, or subskill)
     */
    private String name;
    
    /**
     * Description of the entity
     */
    private String description;
    
    /**
     * Category name - populated for SKILL and SUBSKILL results
     */
    private String categoryName;
    
    /**
     * Parent skill name - populated only for SUBSKILL results
     */
    private String parentSkillName;
    
    /**
     * List of subskill names - populated only for SKILL results
     */
    private List<String> subSkills;
    
    /**
     * Entity status (always ACTIVE for search results)
     */
    private String status;
}
