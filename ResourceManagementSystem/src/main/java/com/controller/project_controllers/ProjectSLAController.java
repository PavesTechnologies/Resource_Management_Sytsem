package com.controller.project_controllers;

import com.dto.ApiResponse;
import com.dto.project_dto.ProjectSLAResponseDTO;
import com.entity.project_entities.ProjectSLA;
import com.entity_enums.client_enums.SLAType;
import com.service_interface.project_service_interface.ProjectSLAService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/project-sla")
public class ProjectSLAController {

    @Autowired
    private ProjectSLAService projectSLAService;

    @PostMapping("/save")
    @PreAuthorize("hasRole('PROJECT_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProjectSLAResponseDTO>> saveProjectSLA(@RequestBody ProjectSLA projectSLA) {
        return projectSLAService.createOrUpdateProjectSLA(projectSLA);
    }

    @DeleteMapping("/{projectSlaId}")
    @PreAuthorize("hasRole('PROJECT_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteProjectSLA(@PathVariable UUID projectSlaId) {
        return projectSLAService.deleteProjectSLA(projectSlaId);
    }

    @GetMapping("/project/{projectId}")
    @PreAuthorize("hasAnyRole('PROJECT_MANAGER', 'ADMIN', 'RESOURCE_MANAGER')")
    public ResponseEntity<ApiResponse<List<ProjectSLAResponseDTO>>> getProjectSLAs(@PathVariable Long projectId) {
        return projectSLAService.getProjectSLAByProjectId(projectId);
    }

    @GetMapping("/project/{projectId}/type/{slaType}")
    @PreAuthorize("hasAnyRole('PROJECT_MANAGER', 'ADMIN', 'RESOURCE_MANAGER')")
    public ResponseEntity<ApiResponse<ProjectSLAResponseDTO>> getProjectSLAByType(
            @PathVariable Long projectId,
            @PathVariable SLAType slaType) {
        return projectSLAService.getProjectSLAByProjectAndType(projectId, slaType);
    }

    @PostMapping("/inherit/{projectId}/type/{slaType}")
    @PreAuthorize("hasRole('PROJECT_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProjectSLAResponseDTO>> inheritClientSLA(
            @PathVariable Long projectId,
            @PathVariable SLAType slaType) {
        return projectSLAService.inheritClientSLA(projectId, slaType);
    }
}
