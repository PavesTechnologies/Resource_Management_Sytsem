package com.controller.client_controllers;


import com.dto.ApiResponse;
import com.entity.client_entities.ClientAssetAssignment;
import com.service_interface.client_service_interface.ClientAssetAssignmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/client-asset-assignments")
public class ClientAssetAssignmentController {
    @Autowired
    private ClientAssetAssignmentService service;

    @PostMapping("/{assetId}")
    @PreAuthorize("hasRole('RESOURCE-MANAGER')")
    public ResponseEntity<?> assignAsset(
            @PathVariable UUID assetId,
            @RequestBody ClientAssetAssignment assignment) {

        return ResponseEntity.ok(
                service.assignAsset(assetId, assignment)
        );
    }

    @PutMapping("/{assignmentId}")
    @PreAuthorize("hasRole('RESOURCE-MANAGER')")
    public ResponseEntity<?> updateAssignment(
            @PathVariable UUID assignmentId,
            @RequestBody ClientAssetAssignment assignment) {

        return ResponseEntity.ok(
                service.updateAssignment(assignmentId, assignment)
        );
    }

    @DeleteMapping("/{assignmentId}")
    @PreAuthorize("hasRole('RESOURCE-MANAGER')")
    public ResponseEntity<?> deleteAssignment(
            @PathVariable UUID assignmentId) {

        return ResponseEntity.ok(
                service.deleteAssignment(assignmentId)
        );
    }

    @GetMapping("/by-asset/{assetId}")
    @PreAuthorize("hasRole('RESOURCE-MANAGER')")
//    public ResponseEntity<?> getAssignmentsByAssetId(@PathVariable Long assetId) {
    public ResponseEntity<?> getAssignmentsByAssetId(@PathVariable UUID assetId) {
        return ResponseEntity.ok(service.getAssignmentsByAssetId(assetId));
    }

    @GetMapping
    @PreAuthorize("hasRole('RESOURCE-MANAGER')")
    public ResponseEntity<?> getAssignments() {

        return ResponseEntity.ok(
                service.getAllAssignments()
        );
    }

    @PutMapping("/return/{assignmentId}")
    @PreAuthorize("hasRole('RESOURCE-MANAGER')")
    public ResponseEntity<?> returnAsset(
            @PathVariable UUID assignmentId,
            @RequestParam("actualReturnDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate actualReturnDate,
            @RequestParam(value = "remarks", required = false)
            String remarks) {

        return ResponseEntity.ok(
                service.returnAsset(assignmentId, actualReturnDate, remarks)
        );
    }

    @GetMapping("/asset/{assetId}")
    @PreAuthorize("hasRole('RESOURCE-MANAGER')")
    public ResponseEntity<ApiResponse<?>> getByAsset(
            @PathVariable UUID assetId) {

        return ResponseEntity.ok(service.getAssignmentsByAssetId(assetId));
    }

}
