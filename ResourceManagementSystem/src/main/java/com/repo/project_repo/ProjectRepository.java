package com.repo.project_repo;


import com.entity.project_entities.Project;
import com.entity_enums.project_enums.ProjectStatus;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
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
        INSERT INTO project (pms_project_id, last_synced_at)
        VALUES (:id, :now)
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

    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO project (pms_project_id, name, last_synced_at, project_status, client_id)
        VALUES (:id, :name, :now, 'ACTIVE', :clientId)
        ON DUPLICATE KEY UPDATE
            last_synced_at = :now
        """, nativeQuery = true)
    void upsertSkeleton(
            @Param("id") Long id,
            @Param("name") String name,
            @Param("now") LocalDateTime now,
            @Param("clientId") UUID clientId
    );

    // 2. Fix: Ensures Instance A locks the row so Instance B waits until the update is finished
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Project p WHERE p.pmsProjectId = :id")
    Optional<Project> findByIdWithLock(@Param("id") Long id);

    Optional<Project> findByPmsProjectId(Long id);

    List<Project> findByClientId(UUID clientId);

    @Query("SELECT p.name FROM Project p WHERE p.projectStatus <> :status")
    List<String> findProjectNamesExceptStatus(
            @Param("status") ProjectStatus status
    );

    @Query("SELECT COUNT(p) FROM Project p WHERE p.projectStatus IN :statuses")
    long countByProjectStatuses(@Param("statuses") List<ProjectStatus> statuses);

    boolean existsByClientIdAndProjectStatus(UUID clientId, ProjectStatus status);

    @Query("SELECT DISTINCT p.primaryLocation FROM Project p WHERE p.projectStatus = :status")
    List<String> findDistinctPrimaryLocationByStatus(@Param("status") ProjectStatus status);
}
