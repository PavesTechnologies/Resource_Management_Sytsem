package com.repo.skill_repo;

import com.entity.skill_entities.SkillCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface SkillCategoryRepository extends JpaRepository<SkillCategory, UUID> {

    boolean existsByNameIgnoreCase(String name);

    List<SkillCategory> findByStatusIgnoreCase(String status);

    @Query("SELECT sc FROM SkillCategory sc WHERE sc.status = 'ACTIVE' ORDER BY sc.name")
    List<SkillCategory> findActiveCategories();

    @Query("SELECT COUNT(s) FROM Skill s WHERE s.category.id = :categoryId AND s.status = 'ACTIVE'")
    long countActiveSkillsByCategoryId(@Param("categoryId") UUID categoryId);

    @Modifying
    @Transactional
    @Query("UPDATE SkillCategory sc SET sc.status = 'INACTIVE' WHERE sc.id = :categoryId")
    int deactivateCategory(@Param("categoryId") UUID categoryId);

    @Query("SELECT sc FROM SkillCategory sc WHERE sc.status = 'ACTIVE' ORDER BY sc.name")
    List<SkillCategory> findSkillTaxonomyTree();

    @Query("SELECT sc FROM SkillCategory sc WHERE sc.id = :categoryId AND sc.status = 'ACTIVE'")
    SkillCategory findActiveCategoryById(@Param("categoryId") UUID categoryId);

    @Query("SELECT s FROM Skill s WHERE s.category.id = :categoryId AND s.status = 'ACTIVE' ORDER BY s.name")
    List<com.entity.skill_entities.Skill> findActiveSkillsByCategoryId(@Param("categoryId") UUID categoryId);

    @Query("SELECT ss FROM SubSkill ss WHERE ss.skill.id IN :skillIds AND ss.status = 'ACTIVE' ORDER BY ss.skill.name, ss.name")
    List<com.entity.skill_entities.SubSkill> findActiveSubSkillsBySkillIds(@Param("skillIds") List<UUID> skillIds);
}

