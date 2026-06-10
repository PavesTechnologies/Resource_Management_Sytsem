package com.repo.project_repo;

import com.entity.project_entities.ProjectCompliance;
import com.entity_enums.client_enums.RequirementType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectComplianceRepo extends JpaRepository<ProjectCompliance, UUID> {
    Optional<List<ProjectCompliance>> findAllByProject_PmsProjectId(Long pmsProjectId);

    Optional<ProjectCompliance> findByProject_PmsProjectIdAndRequirementType(Long pmsProjectId, RequirementType requirementType);
    
    boolean existsByProject_PmsProjectId(Long pmsProjectId);

        boolean existsByProject_PmsProjectIdAndRequirementTypeAndIsInheritedTrue(Long projectId, RequirementType requirementType);

    List<ProjectCompliance> findAllByProject_PmsProjectIdAndActiveFlagTrue(Long projectId);

    @Query("""
        SELECT pc FROM ProjectCompliance pc
        LEFT JOIN FETCH pc.clientCompliance cc
        LEFT JOIN FETCH cc.skill
        LEFT JOIN FETCH cc.certificate
        WHERE pc.project.pmsProjectId = :projectId
        AND pc.activeFlag = true
    """)
    List<ProjectCompliance> findAllByProjectIdWithDetails(@Param("projectId") Long projectId);
}
