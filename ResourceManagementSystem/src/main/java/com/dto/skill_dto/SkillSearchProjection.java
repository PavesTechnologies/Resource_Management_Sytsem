package com.dto.skill_dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Lightweight DTO projection for search results.
 * Used in JPQL constructor expressions to avoid entity hydration.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillSearchProjection {
    
    /**
     * Entity type: CATEGORY, SKILL, or SUBSKILL
     */
    private String type;
    
    /**
     * Unique identifier of the entity
     */
    private UUID id;
    
    /**
     * Name of the entity
     */
    private String name;
    
    /**
     * Description of the entity
     */
    private String description;
    
    /**
     * Category name (null for categories)
     */
    private String categoryName;
    
    /**
     * Parent skill name (null for categories and skills)
     */
    private String parentSkillName;
    
    /**
     * Entity status (always ACTIVE for search results)
     */
    private String status;
}
