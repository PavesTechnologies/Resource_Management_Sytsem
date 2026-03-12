package com.repo.project_repo;

import com.entity.project_entities.ProjectEscalation;
import com.entity_enums.project_enums.EscalationLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectEscalationRepo extends JpaRepository<ProjectEscalation, UUID> {
    List<ProjectEscalation> findByProject_PmsProjectId(Long projectId);
    boolean existsByProject_PmsProjectId(Long projectId);

    Optional<ProjectEscalation> findByProject_PmsProjectIdAndEscalationLevel(Long pmsProjectId, EscalationLevel escalationLevel);
    boolean existsByProject_PmsProjectIdAndContact_ContactId(
            Long projectId,
            UUID contactId
    );

    // Enhanced statistics queries for escalations
    @Query("SELECT COUNT(pe) FROM ProjectEscalation pe JOIN pe.project p WHERE p.clientId = :clientId AND pe.activeFlag = true")
    Long countActiveEscalationsByClientId(@Param("clientId") UUID clientId);

    @Query("SELECT COUNT(pe) FROM ProjectEscalation pe JOIN pe.project p WHERE p.clientId = :clientId AND pe.activeFlag = true AND pe.escalationLevel = 'EXECUTIVE'")
    Long countHighPriorityEscalationsByClientId(@Param("clientId") UUID clientId);

    @Query("SELECT COUNT(pe) FROM ProjectEscalation pe JOIN pe.project p WHERE p.clientId = :clientId AND pe.activeFlag = true AND pe.escalationLevel = 'LEVEL_2'")
    Long countMediumPriorityEscalationsByClientId(@Param("clientId") UUID clientId);

    @Query("SELECT COUNT(pe) FROM ProjectEscalation pe JOIN pe.project p WHERE p.clientId = :clientId AND pe.activeFlag = true AND pe.escalationLevel = 'LEVEL_1'")
    Long countLowPriorityEscalationsByClientId(@Param("clientId") UUID clientId);

    // Duplicate detection methods
    boolean existsByEmailAndProject_PmsProjectId(String email, Long projectId);

    boolean existsByContactNameAndProject_PmsProjectId(String contactName, Long projectId);

    @Query("""
       SELECT pe FROM ProjectEscalation pe
       WHERE pe.project.pmsProjectId = :projectId
       AND pe.email = :email
       AND pe.projectEscalationId != :excludeId
       """)
    Optional<ProjectEscalation> findByEmailAndProjectIdExcludingId(
            @Param("email") String email, 
            @Param("projectId") Long projectId,
            @Param("excludeId") UUID excludeId);

    @Query("""
       SELECT pe FROM ProjectEscalation pe
       WHERE pe.project.pmsProjectId = :projectId
       AND pe.contactName = :contactName
       AND pe.projectEscalationId != :excludeId
       """)
    Optional<ProjectEscalation> findByContactNameAndProjectIdExcludingId(
            @Param("contactName") String contactName, 
            @Param("projectId") Long projectId,
            @Param("excludeId") UUID excludeId);
}
