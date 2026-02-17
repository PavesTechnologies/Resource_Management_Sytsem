package com.repo.skill_repo;

import com.entity.skill_entities.ProficiencyLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ProficiencyLevelRepository extends JpaRepository<ProficiencyLevel, UUID> {

    @Query("SELECT pl FROM ProficiencyLevel pl WHERE pl.activeFlag = true ORDER BY pl.displayOrder")
    List<ProficiencyLevel> findActiveProficiencyLevels();

    boolean existsByProficiencyCodeIgnoreCaseAndProficiencyIdNot(String proficiencyCode, UUID proficiencyId);
}
