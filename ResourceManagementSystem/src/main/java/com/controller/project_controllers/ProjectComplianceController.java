package com.controller.project_controllers;

import com.dto.ApiResponse;
import com.dto.project_dto.ProjectComplianceResponseDTO;
import com.entity.project_entities.ProjectCompliance;
import com.entity_enums.client_enums.RequirementType;
import com.service_interface.project_service_interface.ProjectComplianceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/project-compliance")
public class ProjectComplianceController {

    @Autowired
    private ProjectComplianceService projectComplianceService;

    @PostMapping("/save")
    @PreAuthorize("hasRole('PROJECT_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProjectComplianceResponseDTO>> saveProjectCompliance(@RequestBody ProjectCompliance projectCompliance) {
        return projectComplianceService.createOrUpdateProjectCompliance(projectCompliance);
    }

    @PutMapping("/{projectComplianceId}")
    @PreAuthorize("hasRole('PROJECT_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProjectComplianceResponseDTO>> updateProjectCompliance(
            @PathVariable UUID projectComplianceId, 
            @RequestBody ProjectCompliance projectCompliance) {
        return projectComplianceService.updateProjectCompliance(projectComplianceId, projectCompliance);
    }

    @DeleteMapping("/{projectComplianceId}")
    @PreAuthorize("hasRole('PROJECT_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteProjectCompliance(@PathVariable UUID projectComplianceId) {
        return projectComplianceService.deleteProjectCompliance(projectComplianceId);
    }

    @GetMapping("/project/{projectId}")
    @PreAuthorize("hasAnyRole('PROJECT-MANAGER', 'ADMIN', 'RESOURCE-MANAGER', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<ProjectComplianceResponseDTO>>> getProjectCompliances(@PathVariable Long projectId) {
        return projectComplianceService.getProjectComplianceByProjectId(projectId);
    }

    @GetMapping("/project/{projectId}/type/{complianceType}")
    @PreAuthorize("hasAnyRole('PROJECT_MANAGER', 'ADMIN', 'RESOURCE_MANAGER')")
    public ResponseEntity<ApiResponse<ProjectComplianceResponseDTO>> getProjectComplianceByType(
            @PathVariable Long projectId,
            @PathVariable RequirementType complianceType) {
        return projectComplianceService.getProjectComplianceByProjectAndType(projectId, complianceType);
    }

    @PostMapping("/inherit/{projectId}/type/{complianceType}")
    @PreAuthorize("hasRole('PROJECT_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProjectComplianceResponseDTO>> inheritClientCompliance(
            @PathVariable Long projectId,
            @PathVariable RequirementType complianceType) {
        return projectComplianceService.inheritClientCompliance(projectId, complianceType);
    }

    @PostMapping("/save-all")
    @PreAuthorize("hasRole('PROJECT_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ProjectComplianceResponseDTO>>> saveAllProjectCompliances(
            @RequestBody List<ProjectCompliance> projectCompliances) {
        return projectComplianceService.saveAll(projectCompliances);
    }
}
