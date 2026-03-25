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
    @PreAuthorize("hasAnyRole('RESOURCE-MANAGER', 'PROJECT-MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<?>> getConflictsForResource(@PathVariable Long resourceId) {
        try {
            List<AllocationConflictDTO> conflicts = allocationService.getPendingConflictsForResource(resourceId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Conflicts retrieved successfully", conflicts));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse<>(false, "Error retrieving conflicts: " + e.getMessage(), null));
        }
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('RESOURCE-MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<?>> getPendingConflicts() {
        try {
            List<AllocationConflictDTO> conflicts = allocationService.getAllPendingConflicts();
            return ResponseEntity.ok(new ApiResponse<>(true, "Pending conflicts retrieved successfully", conflicts));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse<>(false, "Error retrieving pending conflicts: " + e.getMessage(), null));
        }
    }

    @PostMapping("/detect/{resourceId}")
    @PreAuthorize("hasAnyRole('RESOURCE-MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<?>> detectConflictsForResource(@PathVariable Long resourceId) {
        try {
            List<AllocationConflictDTO> conflicts = allocationService.detectAllocationConflicts(resourceId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Conflict detection completed", conflicts));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse<>(false, "Error detecting conflicts: " + e.getMessage(), null));
        }
    }

    @PostMapping("/{conflictId}/resolve")
    @PreAuthorize("hasAnyRole('RESOURCE-MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<?>> resolveConflict(
            @PathVariable UUID conflictId,
            @RequestBody ConflictResolutionDTO resolution) {
        
        return allocationService.resolveAllocationConflict(conflictId, resolution);
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('RESOURCE-MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<?>> getConflictStats() {
        try {
            // This can be enhanced with actual stats from the service
            ConflictStats stats = ConflictStats.builder()
                    .totalPending(0) // Would be fetched from service
                    .highSeverityPending(0) // Would be fetched from service
                    .resolvedToday(0) // Would be fetched from service
                    .build();
            
            return ResponseEntity.ok(new ApiResponse<>(true, "Conflict stats retrieved", stats));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse<>(false, "Error retrieving stats: " + e.getMessage(), null));
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
