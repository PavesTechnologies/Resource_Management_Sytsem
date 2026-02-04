package com.repo.project_repo_v;

import com.entity.project_entities_v.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, Long> {

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
}
