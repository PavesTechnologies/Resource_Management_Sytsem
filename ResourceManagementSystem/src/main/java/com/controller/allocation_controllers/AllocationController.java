package com.controller.allocation_controllers;

import com.dto.AllocationRequestDTO;
import com.dto.ApiResponse;
import com.service_interface.allocation_service_interface.AllocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.UUID;

@RestController
@RequestMapping("/api/allocation")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Resource Allocation Management", description = "APIs for managing resource allocations")
public class AllocationController {

    @Autowired
    AllocationService allocationService;

    @PostMapping("/assign")
    @PreAuthorize("hasAnyRole('RESOURCE-MANAGER', 'PROJECT-MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse> assignAllocation(
            @Parameter(description = "Allocation details", required = true)
            @Valid @RequestBody AllocationRequestDTO allocationRequest) {
        
        log.info("API request: Assign allocation for resource {} to {} with percentage {}%", 
                allocationRequest.getResourceId(),
                allocationRequest.getDemandId() != null ? "demand " + allocationRequest.getDemandId() : "project " + allocationRequest.getProjectId(),
                allocationRequest.getAllocationPercentage());
        
        return allocationService.assignAllocation(allocationRequest);
    }

    @GetMapping("/{allocationId}")
    @PreAuthorize("hasAnyRole('RESOURCE-MANAGER', 'PROJECT-MANAGER', 'ADMIN')")
    @Operation(
        summary = "Get allocation by ID",
        description = "Retrieves detailed information about a specific resource allocation"
    )
    public ResponseEntity<ApiResponse> getAllocationById(
            @Parameter(description = "Allocation ID", required = true)
            @PathVariable UUID allocationId) {
        
        log.info("API request: Get allocation by ID {}", allocationId);
        return allocationService.getAllocationById(allocationId);
    }

    @PutMapping("/{allocationId}")
    @PreAuthorize("hasAnyRole('RESOURCE-MANAGER', 'PROJECT-MANAGER', 'ADMIN')")
    @Operation(
        summary = "Update allocation",
        description = "Updates an existing resource allocation. Validates changes for conflicts and availability."
    )
    public ResponseEntity<ApiResponse> updateAllocation(
            @Parameter(description = "Allocation ID", required = true)
            @PathVariable UUID allocationId,
            @Parameter(description = "Updated allocation details", required = true)
            @Valid @RequestBody AllocationRequestDTO allocationRequest) {
        
        log.info("API request: Update allocation {}", allocationId);
        return allocationService.updateAllocation(allocationId, allocationRequest);
    }

    @PostMapping("/{allocationId}/cancel")
    @PreAuthorize("hasAnyRole('RESOURCE-MANAGER', 'PROJECT-MANAGER')")
    public ResponseEntity<ApiResponse> cancelAllocation(
            @Parameter(description = "Allocation ID", required = true)
            @PathVariable UUID allocationId,
            @Parameter(description = "User who is cancelling the allocation", required = true)
            @RequestParam String cancelledBy) {
        
        log.info("API request: Cancel allocation {} by user {}", allocationId, cancelledBy);
        return allocationService.cancelAllocation(allocationId, cancelledBy);
    }

    @GetMapping("/resource/{resourceId}")
    @PreAuthorize("hasAnyRole('RESOURCE-MANAGER', 'PROJECT-MANAGER', 'ADMIN')")
    @Operation(
        summary = "Get allocations by resource",
        description = "Retrieves all allocations for a specific resource"
    )
    public ResponseEntity<ApiResponse> getAllocationsByResource(
            @Parameter(description = "Resource ID", required = true)
            @PathVariable Long resourceId) {
        
        log.info("API request: Get allocations for resource {}", resourceId);
        return allocationService.getAllocationsByResource(resourceId);
    }

    @GetMapping("/demand/{demandId}")
    @PreAuthorize("hasAnyRole('RESOURCE-MANAGER', 'PROJECT-MANAGER', 'ADMIN')")
    @Operation(
        summary = "Get allocations by demand",
        description = "Retrieves all allocations for a specific demand"
    )
    public ResponseEntity<ApiResponse> getAllocationsByDemand(
            @Parameter(description = "Demand ID", required = true)
            @PathVariable UUID demandId) {
        
        log.info("API request: Get allocations for demand {}", demandId);
        return allocationService.getAllocationsByDemand(demandId);
    }

    @GetMapping("/project/{projectId}")
    @PreAuthorize("hasAnyRole('RESOURCE-MANAGER', 'PROJECT-MANAGER', 'ADMIN')")
    @Operation(
        summary = "Get allocations by project",
        description = "Retrieves all allocations for a specific project"
    )
    public ResponseEntity<ApiResponse> getAllocationsByProject(
            @Parameter(description = "Project ID", required = true)
            @PathVariable Long projectId) {
        
        log.info("API request: Get allocations for project {}", projectId);
        return allocationService.getAllocationsByProject(projectId);
    }
}
