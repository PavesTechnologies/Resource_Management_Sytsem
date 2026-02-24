package com.repo.skill_repo;

import com.entity.skill_entities.ProficiencyLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ProficiencyLevelRepository extends JpaRepository<ProficiencyLevel, UUID> {

    @Query("SELECT pl FROM ProficiencyLevel pl WHERE pl.activeFlag = true ORDER BY pl.displayOrder")
    List<ProficiencyLevel> findActiveProficiencyLevels();

    boolean existsByProficiencyCodeIgnoreCaseAndProficiencyIdNot(String proficiencyCode, UUID proficiencyId);

    /**
     * Find proficiency levels by their IDs in a single query
     * Optimized for skill gap matching to avoid N+1 queries
     */
    @Query("SELECT pl FROM ProficiencyLevel pl WHERE pl.id IN :proficiencyIds")
    List<ProficiencyLevel> findByIdIn(@Param("proficiencyIds") List<UUID> proficiencyIds);

    /**
     * Find all active proficiency levels in a single query
     * Use this for caching all proficiency levels at startup
     */
    @Query("SELECT pl FROM ProficiencyLevel pl WHERE pl.activeFlag = true")
    List<ProficiencyLevel> findAllActiveProficiencyLevels();
}
