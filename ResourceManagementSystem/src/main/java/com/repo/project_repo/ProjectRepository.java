package com.repo.project_repo;


import com.entity.project_entities.Project;
import com.entity_enums.project_enums.ProjectStatus;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, Long>, JpaSpecificationExecutor<Project> {

    @Query("SELECT COUNT(p) FROM Project p WHERE p.clientId = :clientId")
    Long countTotalProjectsByClientId(@Param("clientId") UUID clientId);

    @Query("SELECT COUNT(p) FROM Project p WHERE p.clientId = :clientId AND p.projectStatus = :status")
    Long countProjectsByClientIdAndStatus(@Param("clientId") UUID clientId, @Param("status") ProjectStatus status);

    @Query("SELECT COALESCE(SUM(p.projectBudget), 0) FROM Project p WHERE p.clientId = :clientId")
    BigDecimal sumProjectBudgetByClientId(@Param("clientId") UUID clientId);
    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO project (pms_project_id, last_synced_at, name)
        VALUES (:id, :now, 'New Project (Syncing...)')
        ON DUPLICATE KEY UPDATE
            last_synced_at = :now
        """, nativeQuery = true)
    void upsertSkeleton(
            @Param("id") Long id,
            @Param("now") LocalDateTime now
    );

    @Query("""
        SELECT p FROM Project p
        WHERE p.clientId = :clientId
        AND p.pmsProjectId <> :currentProjectId
        AND p.startDate IS NOT NULL
        AND p.endDate IS NOT NULL
        AND (
            (p.startDate BETWEEN :startDate AND :endDate)
            OR
            (p.endDate BETWEEN :startDate AND :endDate)
            OR
            (p.startDate <= :startDate AND p.endDate >= :endDate)
        )
    """)
    List<Project> findOverlappingProjects(
            @Param("clientId") UUID clientId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("currentProjectId") Long currentProjectId
    );

    Page<Project> findByResourceManagerId(
            Long resourceManagerId,
            Pageable pageable
    );
}
