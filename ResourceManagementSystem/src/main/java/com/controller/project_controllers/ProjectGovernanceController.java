package com.controller.project_controllers;

import com.dto.ApiResponse;
import com.dto.ProjectKpiDTO;
import com.dto.UserDTO;
import com.dto.project_dto.*;
import com.entity.project_entities.Project;
import com.entity_enums.centralised_enums.PriorityLevel;
import com.entity_enums.centralised_enums.RiskLevel;
import com.entity_enums.project_enums.ProjectStatus;
import com.entity_enums.project_enums.StaffingReadinessStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.global_exception_handler.ProjectExceptionHandler;
import com.security.CurrentUser;
import com.service_interface.project_service_interface.ProjectGovernanceService;
import com.repo.project_repo.ProjectRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
public class ProjectGovernanceController {

    @Autowired
    private ProjectGovernanceService projectGovernanceService;

    @Autowired
    private ProjectRepository projectRepository;

    // 🔹 STORY 9 — Task 2: Detect overlapping project timelines
    @GetMapping("/{projectId}/overlaps")
    @PreAuthorize("hasAnyRole('RESOURCE-MANAGER','DELIVERY-MANAGER')")
    public ResponseEntity<ApiResponse<List<ProjectOverlapDTO>>> getProjectOverlaps(
            @PathVariable Long projectId) {

        return ResponseEntity.ok(
                projectGovernanceService.getProjectOverlaps(projectId)
        );
    }

    // 🔹 STORY 9 — Task 3: Validate demand dates against project timeline
    @PostMapping("/{projectId}/validate-demand-dates")
    @PreAuthorize("hasAnyRole('RESOURCE-MANAGER','DELIVERY-MANAGER')")
    public ResponseEntity<ApiResponse<DateValidationResponse>> validateDemandDates(
            @PathVariable Long projectId,
            @Valid @RequestBody DemandDateValidationRequest request) {

        return ResponseEntity.ok(
                projectGovernanceService.validateDemandDates(projectId, request)
        );
    }

    // 🔹 STORY 10 — Task 1: Get only eligible projects for demand creation
    @GetMapping("/eligible-for-demand")
    @PreAuthorize("hasAnyRole('RESOURCE-MANAGER','DELIVERY-MANAGER')")
    public ResponseEntity<ApiResponse<List<ProjectListDTO>>> getEligibleProjects() {

        return ResponseEntity.ok(
                projectGovernanceService.getEligibleProjects()
        );
    }

    // 🔹 STORY 10 — Task 3: Get all projects with visibility + eligibility flags
    @GetMapping
    @PreAuthorize("hasAnyRole('RESOURCE-MANAGER','DELIVERY-MANAGER')")
    public ResponseEntity<ApiResponse<List<ProjectListDTO>>> getAllProjectsWithVisibility() {

        return ResponseEntity.ok(
                projectGovernanceService.getAllProjectsWithVisibility()
        );
    }

    // 🔹 STORY 10 — Task 2: Enforce Read-Only Project Integrity
    @PutMapping("/{projectId}")
    @PreAuthorize("hasRole('RESOURCE-MANAGER')")
    public ResponseEntity<ApiResponse<String>> blockProjectUpdate(@PathVariable Long projectId) {

        return ResponseEntity.status(403).body(
                new ApiResponse<>(false,
                        "Project data is read-only in RMS. Please update in PMS.",
                        null)
        );
    }

    @GetMapping("get-projects")
    @PreAuthorize("hasAnyRole('RESOURCE-MANAGER','DELIVERY-MANAGER')")
    public ResponseEntity<ApiResponse<Page<ProjectsListDTO>>> getProjectsByManagerId(
            @CurrentUser UserDTO userDTO,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) StaffingReadinessStatus readinessStatus,
            @RequestParam(required = false) ProjectStatus projectStatus,
            @RequestParam(required = false) PriorityLevel priorityLevel,
            @RequestParam(required = false) RiskLevel riskLevel
    ) {
        Long managerId = userDTO.getId();

        return projectGovernanceService.getProjectsByManagerId(
                managerId,
                page,
                size,
                search,
                readinessStatus,
                projectStatus,
                priorityLevel,
                riskLevel
        );
    }

    @GetMapping("/get-project-by-id/{id}")
    @PreAuthorize("hasAnyRole('RESOURCE-MANAGER','DELIVERY-MANAGER', 'MANAGER')")
    public ResponseEntity<ApiResponse<?>> getProjectById(@PathVariable Long id) {
        return projectGovernanceService.getProjectById(id);
    }

    @GetMapping("/check-demand-creation/{pmsProjectId}")
    @PreAuthorize("hasAnyRole('RESOURCE-MANAGER','DELIVERY-MANAGER', 'MANAGER')")
    public ResponseEntity<ApiResponse<?>> checkDemandCreation(@PathVariable Long pmsProjectId) {
        return projectGovernanceService.checkDemandCreation(pmsProjectId);
    }

    @PutMapping("/readiness-status-update")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESOURCE-MANAGER')")
    public ResponseEntity<ApiResponse<?>> changeReadinessStatus(@RequestBody String requestBody) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            
            // Try to parse as wrapped object first
            JsonNode rootNode = mapper.readTree(requestBody);
            
            UpdateReadinessStatusDTO readiness;
            if (rootNode.has("readinessData")) {
                // Extract the inner object
                JsonNode readinessDataNode = rootNode.get("readinessData");
                readiness = mapper.treeToValue(readinessDataNode, UpdateReadinessStatusDTO.class);
            } else {
                // Parse directly
                readiness = mapper.readValue(requestBody, UpdateReadinessStatusDTO.class);
            }
            
            return projectGovernanceService.readinessStatusUpdate(readiness);
        } catch (Exception e) {
            throw new ProjectExceptionHandler(HttpStatus.BAD_REQUEST, "400", "Invalid JSON format: " + e.getMessage());
        }
    }
    @GetMapping("/get-project-by-client-id/{clientId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESOURCE-MANAGER')")
    public ResponseEntity<ApiResponse<?>> getProjectByClientId(@PathVariable UUID clientId) {
        return projectGovernanceService.getProjectByClient(clientId);
    }

    @GetMapping("/get-locations")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESOURCE-MANAGER', 'PROJECT-MANAGER')")
    public ResponseEntity<?> getLocations() {
        return projectGovernanceService.getLocationsByStatus();
    }

    // Project KPI endpoint
    @GetMapping("/kpi")
    @PreAuthorize("hasRole('RESOURCE-MANAGER')")
    public ResponseEntity<ApiResponse<ProjectKpiDTO>> getProjectKpi() {
        try {
            // Total Projects count
            Long totalProjects = projectRepository.count();

            // Active Projects count
            Long activeProjects = projectRepository.countByProjectStatus(ProjectStatus.ACTIVE);

            // High Risk Projects count
            Long highRiskProjects = projectRepository.countByRiskLevel(RiskLevel.HIGH);

            // Calculate Average Resource Utilization
            // For now, we'll calculate this as (Active Projects / Total Projects) * 100
            // This can be enhanced later with actual resource allocation data
            Double avgResourceUtil = 0.0;
            if (totalProjects != null && totalProjects > 0) {
                avgResourceUtil = (double) (activeProjects * 100) / totalProjects;
            }

            ProjectKpiDTO kpiDTO = new ProjectKpiDTO(
                totalProjects != null ? totalProjects : 0L,
                activeProjects != null ? activeProjects : 0L,
                highRiskProjects != null ? highRiskProjects : 0L,
                avgResourceUtil
            );

            return ResponseEntity.ok(
                new ApiResponse<>(true, "Project KPI data retrieved successfully", kpiDTO)
            );

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ApiResponse<>(false, "Failed to retrieve project KPI data: " + e.getMessage(), null)
            );
        }
    }

}
