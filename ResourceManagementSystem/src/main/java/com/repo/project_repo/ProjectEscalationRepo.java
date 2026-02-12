package com.repo.project_repo;

import com.entity.project_entities.ProjectEscalation;
import com.entity_enums.project_enums.EscalationLevel;
import org.springframework.data.jpa.repository.JpaRepository;
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

}
