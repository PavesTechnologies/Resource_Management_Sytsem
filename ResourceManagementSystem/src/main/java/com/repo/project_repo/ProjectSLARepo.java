package com.repo.project_repo;

import com.entity.project_entities.ProjectSLA;
import com.entity_enums.client_enums.SLAType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectSLARepo extends JpaRepository<ProjectSLA, UUID> {
    Optional<List<ProjectSLA>> findAllByProject_ProjectId(UUID projectId);

    Optional<ProjectSLA> findByProject_PmsProjectIdAndSlaType(Long pmsProjectId, SLAType slaType);
}