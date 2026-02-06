package com.repo.project_repo;

import com.entity.project_entities.ProjectCompliance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProjectComplianceRepo extends JpaRepository<ProjectCompliance, UUID> {
    List<ProjectCompliance> findByProject_PmsProjectId(Long projectId);
    boolean existsByProject_PmsProjectId(Long projectId);
}
