package com.controller.client_controllers;


import com.dto.ApiResponse;
import com.entity.client_entities.ResourceEnablementAssignment;
import com.entity_enums.client_enums.EnablementAssignmentStatus;
import com.service_interface.client_service_interface.ResourceEnablementAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/resource-enablements")
public class ResourceEnablementAssignmentController {
    private final ResourceEnablementAssignmentService service;

    @PostMapping("/request")
    public ResponseEntity<ApiResponse<String>> requestEnablement(
            @RequestBody ResourceEnablementAssignment assignment) {

        ApiResponse<String> response = service.requestEnablement(assignment);

        return ResponseEntity
                .status(response.getSuccess() ? HttpStatus.CREATED : HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @PutMapping("/{assignmentId}/status")
    public ResponseEntity<ApiResponse<String>> updateStatus(
            @PathVariable UUID assignmentId,
            @RequestParam EnablementAssignmentStatus status,
            @RequestParam(required = false) String remarks) {

        ApiResponse<String> response =
                service.updateStatus(assignmentId, status, remarks);

        return ResponseEntity.ok(response);
    }
}
