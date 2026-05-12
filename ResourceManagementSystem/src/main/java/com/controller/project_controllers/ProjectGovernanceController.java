package com.controller.project_controllers;

import com.dto.centralised_dto.ApiResponse;
import com.dto.project_dto.ProjectKpiDTO;
import com.dto.centralised_dto.UserDTO;
import com.dto.project_dto.*;
import com.entity_enums.centralised_enums.PriorityLevel;
import com.entity_enums.centralised_enums.RiskLevel;
import com.entity_enums.project_enums.ProjectStatus;
import com.entity_enums.project_enums.StaffingReadinessStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.global_exception_handler.ProjectExceptionHandler;
import com.security.CurrentUser;
import com.service_interface.project_service_interface.ProjectGovernanceService;
import com.repo.project_repo.ProjectRepository;
import com.repo.allocation_repo.AllocationRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.extern.slf4j.Slf4j;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
@Slf4j
public class ProjectGovernanceController {

    @Autowired
    private ProjectGovernanceService projectGovernanceService;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private AllocationRepository allocationRepository;

    // 🔹 STORY 9 — Task 2: Detect overlapping project timelines
    @GetMapping("/{projectId}/overlaps")
    @PreAuthorize("hasAnyRole('Resource_Manager','Delivery_Manager')")
    public ResponseEntity<ApiResponse<List<ProjectOverlapDTO>>> getProjectOverlaps(
            @PathVariable Long projectId) {

        return ResponseEntity.ok(
                projectGovernanceService.getProjectOverlaps(projectId)
        );
    }

    // 🔹 STORY 9 — Task 3: Validate demand dates against project timeline
    @PostMapping("/{projectId}/validate-demand-dates")
    @PreAuthorize("hasAnyRole('Resource_Manager','Delivery_Manager')")
    public ResponseEntity<ApiResponse<DateValidationResponse>> validateDemandDates(
            @PathVariable Long projectId,
            @Valid @RequestBody DemandDateValidationRequest request) {

        return ResponseEntity.ok(
                projectGovernanceService.validateDemandDates(projectId, request)
        );
    }

    // 🔹 STORY 10 — Task 1: Get only eligible projects for demand creation
    @GetMapping("/eligible-for-demand")
    @PreAuthorize("hasAnyRole('Resource_Manager','Delivery_Manager')")
    public ResponseEntity<ApiResponse<List<ProjectListDTO>>> getEligibleProjects() {

        return ResponseEntity.ok(
                projectGovernanceService.getEligibleProjects()
        );
    }

    // 🔹 STORY 10 — Task 3: Get all projects with visibility + eligibility flags
    @GetMapping
    @PreAuthorize("hasAnyRole('Resource_Manager','Delivery_Manager')")
    public ResponseEntity<ApiResponse<List<ProjectListDTO>>> getAllProjectsWithVisibility() {

        return ResponseEntity.ok(
                projectGovernanceService.getAllProjectsWithVisibility()
        );
    }

    // 🔹 STORY 10 — Task 2: Enforce Read-Only Project Integrity
    @PutMapping("/{projectId}")
    @PreAuthorize("hasRole('Resource_Manager')")
    public ResponseEntity<ApiResponse<String>> blockProjectUpdate(@PathVariable Long projectId) {

        return ResponseEntity.status(403).body(
                new ApiResponse<>(false,
                        "Project data is read-only in RMS. Please update in PMS.",
                        null)
        );
    }

    @GetMapping("get-projects")
    @PreAuthorize("hasAnyRole('Resource_Manager','Delivery_Manager')")
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
    @PreAuthorize("hasAnyRole('Resource_Manager','Delivery_Manager','Project_Manager')")
    public ResponseEntity<ApiResponse<?>> getProjectById(@PathVariable Long id) {
        return projectGovernanceService.getProjectById(id);
    }

    @GetMapping("/check-demand-creation/{pmsProjectId}")
    @PreAuthorize("hasAnyRole('Resource_Manager','Delivery_Manager','Project_Manager')")
    public ResponseEntity<ApiResponse<?>> checkDemandCreation(@PathVariable Long pmsProjectId) {
        return projectGovernanceService.checkDemandCreation(pmsProjectId);
    }

    @PutMapping("/readiness-status-update")
    @PreAuthorize("hasAnyRole('Admin', 'Resource_Manager', 'Delivery_Manager')")
    public ResponseEntity<ApiResponse<?>> changeReadinessStatus(@RequestBody String requestBody) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            
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
    @PreAuthorize("hasAnyRole('Admin', 'Resource_Manager')")
    public ResponseEntity<ApiResponse<?>> getProjectByClientId(@PathVariable UUID clientId) {
        return projectGovernanceService.getProjectByClient(clientId);
    }

    @GetMapping("/get-locations")
    @PreAuthorize("hasAnyRole('Admin', 'Resource_Manager', 'Project_Manager')")
    public ResponseEntity<?> getLocations() {
        return projectGovernanceService.getLocationsByStatus();
    }

    // Project KPI endpoint
    @GetMapping("/kpi")
    @PreAuthorize("hasRole('Resource_Manager')")
    public ResponseEntity<ApiResponse<ProjectKpiDTO>> getProjectKpi() {
        try {
            // Total Projects count (excluding completed projects)
            List<ProjectStatus> nonCompletedStatuses = List.of(
                ProjectStatus.ACTIVE,
                ProjectStatus.APPROVED,
                ProjectStatus.ARCHIVED,
                ProjectStatus.PLANNING
            );
            Long totalProjects = projectRepository.countByProjectStatuses(nonCompletedStatuses);

            // Active Projects count
            Long activeProjects = projectRepository.countByProjectStatus(ProjectStatus.ACTIVE);

            // High Risk Projects count
            Long highRiskProjects = projectRepository.countByRiskLevel(RiskLevel.HIGH);

            // Calculate Average Resource Utilization using real allocation data
            Double avgResourceUtil = 0.0;
            String calculationSource = "default";
            
            try {
                // Get real average utilization from active allocations
                Double realUtilization = allocationRepository.calculateAverageUtilization();
                if (realUtilization != null) {
                    avgResourceUtil = realUtilization;
                    calculationSource = "real_allocation_data";
                }
            } catch (Exception e) {
                // Fallback to previous calculation if real data fails
                if (totalProjects != null && totalProjects > 0) {
                    avgResourceUtil = (double) (activeProjects * 100) / totalProjects;
                    calculationSource = "fallback_project_ratio";
                }
            }
            
            // Add debug info to response for verification (temporary)
            System.out.println("DEBUG: Utilization calculation source: " + calculationSource);
            System.out.println("DEBUG: Final utilization value: " + avgResourceUtil + "%");

            // Create ProjectKpiDTO with the calculated KPI data
            com.dto.project_dto.ProjectKpiDTO projectKpiDTO = new com.dto.project_dto.ProjectKpiDTO();
            projectKpiDTO.setTotalProjects(totalProjects != null ? totalProjects : 0L);
            projectKpiDTO.setActiveProjects(activeProjects != null ? activeProjects : 0L);
            projectKpiDTO.setHighRiskProjects(highRiskProjects != null ? highRiskProjects : 0L);
            projectKpiDTO.setAvgResourceUtil(avgResourceUtil);

            // Add debug info to message for verification
            String message = "Project KPI data retrieved successfully (Source: " + calculationSource + ", Utilization: " + avgResourceUtil + "%)";
            return ResponseEntity.ok(
                new ApiResponse<>(true, message, projectKpiDTO)
            );

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ApiResponse<>(false, "Failed to retrieve project KPI data: " + e.getMessage(), null)
            );
        }
    }

}
