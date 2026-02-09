package com.repo.project_repo;

import com.entity.project_entities.ProjectCompliance;
import com.entity_enums.client_enums.ComplianceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectComplianceRepo extends JpaRepository<ProjectCompliance, UUID> {
    Optional<List<ProjectCompliance>> findAllByProject_PmsProjectId(Long pmsProjectId);

    Optional<ProjectCompliance> findByProject_PmsProjectIdAndComplianceType(Long pmsProjectId, ComplianceType complianceType);
    
    boolean existsByProject_PmsProjectId(Long pmsProjectId);
}
