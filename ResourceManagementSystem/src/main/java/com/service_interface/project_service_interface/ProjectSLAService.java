package com.service_interface.project_service_interface;

import com.dto.ApiResponse;
import com.entity.project_entities.ProjectSLA;
import com.entity_enums.client_enums.SLAType;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

public interface ProjectSLAService {
    ResponseEntity<ApiResponse> createOrUpdateProjectSLA(ProjectSLA projectSLA);
    ResponseEntity<ApiResponse> deleteProjectSLA(UUID projectSlaId);
    ResponseEntity<ApiResponse> getProjectSLAByProjectId(UUID projectId);
    ResponseEntity<ApiResponse> getProjectSLAByProjectAndType(Long projectId, SLAType slaType);
    ResponseEntity<ApiResponse> inheritClientSLA(Long projectId, SLAType slaType);
}
