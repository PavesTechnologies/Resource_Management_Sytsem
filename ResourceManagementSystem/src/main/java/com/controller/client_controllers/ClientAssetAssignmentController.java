package com.controller.client_controllers;


import com.dto.ApiResponse;
import com.dto.AssetAssignmentKPIDTo;
import com.entity.client_entities.ClientAssetAssignment;
import com.service_interface.client_service_interface.ClientAssetAssignmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/client-asset-assignments")
public class ClientAssetAssignmentController {
    @Autowired
    private ClientAssetAssignmentService service;

    @PostMapping("/{assetId}")
    public ResponseEntity<?> assignAsset(
            @PathVariable UUID assetId,
            @RequestBody ClientAssetAssignment assignment) {

        return service.assignAsset(assetId, assignment);
    }

    @PutMapping("/{assignmentId}")
    public ResponseEntity<?> updateAssignment(
            @PathVariable UUID assignmentId,
            @RequestBody ClientAssetAssignment assignment) {

        return ResponseEntity.ok(
                service.updateAssignment(assignmentId, assignment)
        );
    }

    @DeleteMapping("/{assignmentId}")
    public ResponseEntity<?> deleteAssignment(
            @PathVariable UUID assignmentId) {

        return service.deleteAssignment(assignmentId);
    }

    @GetMapping("/by-asset/{assetId}")
    public ResponseEntity<?> getAssignmentsByAssetId(@PathVariable UUID assetId) {
        return ResponseEntity.ok(service.getAssignmentsByAssetId(assetId));
    }

    @GetMapping
    public ResponseEntity<?> getAssignments() {

        return ResponseEntity.ok(
                service.getAllAssignments()
        );
    }

    @PutMapping("/return/{assignmentId}")
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
    public ResponseEntity<ApiResponse<?>> getByAsset(
            @PathVariable UUID assetId) {

        return ResponseEntity.ok(service.getAssignmentsByAssetId(assetId));
    }

    @GetMapping("/kpi/{assetId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESOURCE_MANAGER')")
    public ResponseEntity<ApiResponse<AssetAssignmentKPIDTo>> getKPI(@PathVariable UUID assetId) {
        return service.getKPI(assetId);
    }
}
