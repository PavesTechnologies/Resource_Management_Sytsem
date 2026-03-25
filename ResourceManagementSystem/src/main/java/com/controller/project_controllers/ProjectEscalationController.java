package com.controller.project_controllers;

import com.dto.centralised_dto.ApiResponse;
import com.dto.project_dto.ProjectEscalationResponseDTO;
import com.entity.project_entities.ProjectEscalation;
import com.service_interface.project_service_interface.ProjectEscalationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectEscalationController {

    private final ProjectEscalationService projectEscalationService;

    @PostMapping("/escalations/save")
    @PreAuthorize("hasRole('PROJECT-MANAGER')")
    public ResponseEntity<ApiResponse<?>> addEscalationContact(@Valid @RequestBody ProjectEscalationResponseDTO escalation) {
        return projectEscalationService.addEscalationContact(escalation);
    }

    @GetMapping("/{projectId}/escalations")
    public ResponseEntity<ApiResponse<?>> getEscalationContacts(@PathVariable Long projectId) {
        return projectEscalationService.getEscalationContacts(projectId);
    }
    @PutMapping("/update-escalation/{projectEscalationId}")
    public ResponseEntity<ApiResponse<?>> updateProjectContact(@PathVariable UUID projectEscalationId, @Valid @RequestBody ProjectEscalation escalation) {
        return projectEscalationService.updateProjectContact(projectEscalationId, escalation);
    }

    @DeleteMapping("/delete-escalation/{projectEscalationId}")
    public ResponseEntity<ApiResponse<?>> deleteProjectContact(@PathVariable UUID projectEscalationId) {
        return projectEscalationService.deleteProjectContact(projectEscalationId);
    }

//    @GetMapping("/{projectId}/escalations")
//    @Operation(summary = "Get all escalation contacts for a project", description = "Retrieves a list of all mapped escalation contacts for a specific project.")
//    public ResponseEntity<ApiResponse<List<ProjectEscalationDTO>>> getEscalationContacts(@PathVariable Long projectId) {
//        List<ProjectEscalationDTO> contacts = projectEscalationService.getEscalationContacts(projectId);
//        return new ResponseEntity<>(new ApiResponse<>(true, "Escalation contacts retrieved successfully", contacts), HttpStatus.OK);
//    }
//
//    @DeleteMapping("/escalations/{escalationId}")
//    @Operation(summary = "Remove an escalation contact mapping", description = "Removes a specific escalation contact mapping from a project.")
//    public ResponseEntity<ApiResponse<Void>> removeEscalationContact(@PathVariable UUID escalationId) {
//        projectEscalationService.removeEscalationContact(escalationId);
//        return new ResponseEntity<>(new ApiResponse<>(true, "Escalation contact removed successfully", null), HttpStatus.OK);
//    }
}
