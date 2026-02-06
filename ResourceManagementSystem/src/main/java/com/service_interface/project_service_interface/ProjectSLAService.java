package com.service_interface.project_service_interface;

import com.dto.ApiResponse;
import com.entity.project_entities.ProjectSLA;
import com.entity_enums.client_enums.SLAType;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

public interface ProjectSLAService {
    ResponseEntity<ApiResponse<ProjectSLA>> createOrUpdateProjectSLA(ProjectSLA projectSLA);
    ResponseEntity<ApiResponse<Void>> deleteProjectSLA(UUID projectSlaId);
    ResponseEntity<ApiResponse<List<ProjectSLA>>> getProjectSLAByProjectId(Long projectId);
    ResponseEntity<ApiResponse<ProjectSLA>> getProjectSLAByProjectAndType(Long projectId, SLAType slaType);
    ResponseEntity<ApiResponse<ProjectSLA>> inheritClientSLA(Long projectId, SLAType slaType);
}
