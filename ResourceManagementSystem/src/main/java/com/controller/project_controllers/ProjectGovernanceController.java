package com.controller.project_controllers;

import com.dto.ApiResponse;
import com.dto.UserDTO;
import com.dto.project_dto.DateValidationResponse;
import com.dto.project_dto.DemandDateValidationRequest;
import com.dto.project_dto.ProjectListDTO;
import com.dto.project_dto.ProjectOverlapDTO;
import com.entity.project_entities.Project;
import com.security.CurrentUser;
import com.service_interface.project_service_interface.ProjectGovernanceService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectGovernanceController {

    @Autowired
    private ProjectGovernanceService projectGovernanceService;

    // 🔹 STORY 9 — Task 2: Detect overlapping project timelines
    @GetMapping("/{projectId}/overlaps")
    @PreAuthorize("hasRole('RESOURCE-MANAGER')")
    public ResponseEntity<ApiResponse<List<ProjectOverlapDTO>>> getProjectOverlaps(
            @PathVariable Long projectId) {

        return ResponseEntity.ok(
                projectGovernanceService.getProjectOverlaps(projectId)
        );
    }

    // 🔹 STORY 9 — Task 3: Validate demand dates against project timeline
    @PostMapping("/{projectId}/validate-demand-dates")
    @PreAuthorize("hasRole('RESOURCE-MANAGER')")
    public ResponseEntity<ApiResponse<DateValidationResponse>> validateDemandDates(
            @PathVariable Long projectId,
            @Valid @RequestBody DemandDateValidationRequest request) {

        return ResponseEntity.ok(
                projectGovernanceService.validateDemandDates(projectId, request)
        );
    }

    // 🔹 STORY 10 — Task 1: Get only eligible projects for demand creation
    @GetMapping("/eligible-for-demand")
    @PreAuthorize("hasRole('RESOURCE-MANAGER')")
    public ResponseEntity<ApiResponse<List<ProjectListDTO>>> getEligibleProjects() {

        return ResponseEntity.ok(
                projectGovernanceService.getEligibleProjects()
        );
    }

    // 🔹 STORY 10 — Task 3: Get all projects with visibility + eligibility flags
    @GetMapping
    @PreAuthorize("hasRole('RESOURCE-MANAGER')")
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
    @PreAuthorize("hasRole('RESOURCE-MANAGER')")
    public ResponseEntity<ApiResponse<List<Project>>> getProjectsByManagerId(@CurrentUser UserDTO userDTO) {
        Long managerId=userDTO.getId();

        return ResponseEntity.ok(
                projectGovernanceService.getProjectsByManagerId(managerId)
        );
    }
}
