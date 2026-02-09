package com.service_interface.project_service_interface;

import com.dto.ApiResponse;
import com.dto.project_dto.ProjectSLAResponseDTO;
import com.entity.project_entities.ProjectSLA;
import com.entity_enums.client_enums.SLAType;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

public interface ProjectSLAService {
    ResponseEntity<ApiResponse<ProjectSLAResponseDTO>> createOrUpdateProjectSLA(ProjectSLA projectSLA);
    ResponseEntity<ApiResponse<ProjectSLAResponseDTO>> updateProjectSLA(UUID projectSlaId, ProjectSLA projectSLA);
    ResponseEntity<ApiResponse<Void>> deleteProjectSLA(UUID projectSlaId);
    ResponseEntity<ApiResponse<List<ProjectSLAResponseDTO>>> getProjectSLAByProjectId(Long projectId);
    ResponseEntity<ApiResponse<ProjectSLAResponseDTO>> getProjectSLAByProjectAndType(Long projectId, SLAType slaType);
    ResponseEntity<ApiResponse<ProjectSLAResponseDTO>> inheritClientSLA(Long projectId, SLAType slaType);
    ResponseEntity<ApiResponse<List<ProjectSLAResponseDTO>>> saveAll(List<ProjectSLA> projectSLAs);
}
