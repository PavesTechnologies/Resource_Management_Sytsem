package com.controller.allocation_controllers;

import com.dto.allocation_dto.AllocationConflictDTO;
import com.dto.allocation_dto.ConflictResolutionDTO;
import com.dto.centralised_dto.ApiResponse;
import com.service_interface.allocation_service_interface.AllocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/allocation-conflicts")
@RequiredArgsConstructor
public class AllocationConflictController {

    private final AllocationService allocationService;

    @GetMapping("/resource/{resourceId}")
    @PreAuthorize("hasAnyRole('Resource_Manager', 'Project_Manager', 'Admin')")
    public ResponseEntity<ApiResponse<List<AllocationConflictDTO>>> getConflictsForResource(@PathVariable String resourceId) {
        try {
            List<AllocationConflictDTO> conflicts = allocationService.getPendingConflictsForResource(resourceId);
            return ResponseEntity.ok(ApiResponse.success("Conflicts retrieved successfully", conflicts));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error retrieving conflicts: " + e.getMessage()));
        }
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('Resource_Manager', 'Admin')")
    public ResponseEntity<ApiResponse<List<AllocationConflictDTO>>> getPendingConflicts() {
        try {
            List<AllocationConflictDTO> conflicts = allocationService.getAllPendingConflicts();
            return ResponseEntity.ok(ApiResponse.success("Pending conflicts retrieved successfully", conflicts));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error retrieving pending conflicts: " + e.getMessage()));
        }
    }

    @PostMapping("/detect/{resourceId}")
    @PreAuthorize("hasAnyRole('Resource_Manager', 'Admin')")
    public ResponseEntity<ApiResponse<List<AllocationConflictDTO>>> detectConflictsForResource(@PathVariable String resourceId) {
        try {
            List<AllocationConflictDTO> conflicts = allocationService.detectAllocationConflicts(resourceId);
            return ResponseEntity.ok(ApiResponse.success("Conflict detection completed", conflicts));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error detecting conflicts: " + e.getMessage()));
        }
    }

    @PostMapping("/{conflictId}/resolve")
    @PreAuthorize("hasAnyRole('Resource_Manager', 'Admin')")
    public ResponseEntity<ApiResponse<?>> resolveConflict(
            @PathVariable UUID conflictId,
            @RequestBody ConflictResolutionDTO resolution) {
        
        return allocationService.resolveAllocationConflict(conflictId, resolution);
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('Resource_Manager', 'Admin')")
    public ResponseEntity<ApiResponse<ConflictStats>> getConflictStats() {
        try {
            // This can be enhanced with actual stats from the service
            ConflictStats stats = ConflictStats.builder()
                    .totalPending(0) // Would be fetched from service
                    .highSeverityPending(0) // Would be fetched from service
                    .resolvedToday(0) // Would be fetched from service
                    .build();
            
            return ResponseEntity.ok(ApiResponse.success("Conflict stats retrieved", stats));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error retrieving stats: " + e.getMessage()));
        }
    }

    // Simple stats DTO for the endpoint above
    @lombok.Data
    @lombok.Builder
    private static class ConflictStats {
        private long totalPending;
        private long highSeverityPending;
        private long resolvedToday;
    }
}
