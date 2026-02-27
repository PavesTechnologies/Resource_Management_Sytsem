package com.repo.project_repo;

import com.entity.project_entities.Project;
import com.entity.project_entities.ProjectSLA;
import com.entity_enums.client_enums.SLAType;
import com.entity_enums.demand_enums.DemandType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectSLARepo extends JpaRepository<ProjectSLA, UUID> {
    Optional<List<ProjectSLA>> findAllByProject_PmsProjectId(Long pmsProjectId);

    Optional<ProjectSLA> findByProject_PmsProjectIdAndSlaType(Long pmsProjectId, SLAType slaType);
    
    boolean existsByProject_PmsProjectId(Long pmsProjectId);
    
    boolean existsByClientSLA_SlaIdAndActiveFlagTrue(UUID clientSlaId);
    
    List<ProjectSLA> findByClientSLA_SlaIdAndActiveFlagTrue(UUID clientSlaId);

    Optional<ProjectSLA> findByProjectAndSlaTypeAndActiveFlagTrue(Project project, DemandType slaType);
}
