package com.service_interface.project_service_interface;

import com.dto.ApiResponse;
import com.dto.project_dto.ProjectComplianceResponseDTO;
import com.entity.project_entities.ProjectCompliance;
import com.entity_enums.client_enums.ComplianceType;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

public interface ProjectComplianceService {
    ResponseEntity<ApiResponse<ProjectComplianceResponseDTO>> createOrUpdateProjectCompliance(ProjectCompliance projectCompliance);
    ResponseEntity<ApiResponse<Void>> deleteProjectCompliance(UUID projectComplianceId);
    ResponseEntity<ApiResponse<List<ProjectComplianceResponseDTO>>> getProjectComplianceByProjectId(Long projectId);
    ResponseEntity<ApiResponse<ProjectComplianceResponseDTO>> getProjectComplianceByProjectAndType(Long projectId, ComplianceType complianceType);
    ResponseEntity<ApiResponse<ProjectComplianceResponseDTO>> inheritClientCompliance(Long projectId, ComplianceType complianceType);
    ResponseEntity<ApiResponse<List<ProjectComplianceResponseDTO>>> saveAll(List<ProjectCompliance> projectCompliances);
}
