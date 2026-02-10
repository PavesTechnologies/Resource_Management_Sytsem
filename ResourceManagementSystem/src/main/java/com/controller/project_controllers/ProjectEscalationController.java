package com.controller.project_controllers;

import com.dto.ApiResponse;
import com.dto.project_dto.ProjectEscalationDTO;
import com.dto.project_dto.ProjectEscalationResponseDTO;
import com.entity.project_entities.ProjectEscalation;
import com.service_interface.project_service_interface.ProjectEscalationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Tag(name = "Project Escalation Management", description = "APIs for managing project escalation contacts and rules")
public class ProjectEscalationController {

    private final ProjectEscalationService projectEscalationService;

    @PostMapping("/escalations")
    @Operation(summary = "Map a client escalation contact to a project", description = "Assigns a client contact to a project with a specific escalation level and triggers.")
    public ResponseEntity<?> addEscalationContact(@Valid @RequestBody ProjectEscalationResponseDTO escalation) {
        return projectEscalationService.addEscalationContact(escalation);
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
