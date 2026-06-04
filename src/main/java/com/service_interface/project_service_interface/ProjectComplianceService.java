package com.service_interface.project_service_interface;

import com.dto.centralised_dto.ApiResponse;
import com.dto.project_dto.ProjectComplianceResponseDTO;
import com.entity.project_entities.ProjectCompliance;
import com.entity_enums.client_enums.RequirementType;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

public interface ProjectComplianceService {
    ResponseEntity<ApiResponse<ProjectComplianceResponseDTO>> createOrUpdateProjectCompliance(ProjectCompliance projectCompliance);
    ResponseEntity<ApiResponse<ProjectComplianceResponseDTO>> updateProjectCompliance(UUID projectComplianceId, ProjectCompliance projectCompliance);
    ResponseEntity<ApiResponse<Void>> deleteProjectCompliance(UUID projectComplianceId);
    ResponseEntity<ApiResponse<List<ProjectComplianceResponseDTO>>> getProjectComplianceByProjectId(Long projectId);
    ResponseEntity<ApiResponse<ProjectComplianceResponseDTO>> getProjectComplianceByProjectAndType(Long projectId, RequirementType requirementType);
    ResponseEntity<ApiResponse<ProjectComplianceResponseDTO>> inheritClientCompliance(Long projectId, RequirementType requirementType);
    ResponseEntity<ApiResponse<List<ProjectComplianceResponseDTO>>> saveAll(List<ProjectCompliance> projectCompliances);
}
