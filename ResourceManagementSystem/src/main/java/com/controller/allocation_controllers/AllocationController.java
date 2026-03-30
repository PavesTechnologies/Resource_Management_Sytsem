package com.controller.allocation_controllers;

import com.dto.allocation_dto.AllocationRequestDTO;
import com.dto.allocation_dto.InternalPoolAllocationApproval;
import com.dto.centralised_dto.ApiResponse;
import com.dto.centralised_dto.UserDTO;
import com.dto.allocation_dto.CloseAllocationDTO;
import com.security.CurrentUser;
import com.service_interface.allocation_service_interface.AllocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.UUID;

@RestController
@RequestMapping("/api/allocation")
@RequiredArgsConstructor
public class AllocationController {

    private final AllocationService allocationService;

    @PostMapping("/assign")
    @PreAuthorize("hasAnyRole('RESOURCE-MANAGER', 'PROJECT-MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<?>> assignAllocation(
            @Valid @RequestBody AllocationRequestDTO allocationRequest, @CurrentUser UserDTO user) {
        allocationRequest.setCreatedBy(user.getName());
        return allocationService.assignAllocation(allocationRequest);
    }

    @GetMapping("/{allocationId}")
    @PreAuthorize("hasAnyRole('RESOURCE-MANAGER', 'PROJECT-MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<?>> getAllocationById(
            @PathVariable UUID allocationId) {
        return allocationService.getAllocationById(allocationId);
    }

    @PutMapping("/{allocationId}")
    @PreAuthorize("hasAnyRole('RESOURCE-MANAGER', 'PROJECT-MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<?>> updateAllocation(
            @PathVariable UUID allocationId,
            @Valid @RequestBody AllocationRequestDTO allocationRequest) {
        return allocationService.updateAllocation(allocationId, allocationRequest);
    }

    @PostMapping("/{allocationId}/cancel")
    @PreAuthorize("hasAnyRole('RESOURCE-MANAGER', 'PROJECT-MANAGER')")
    public ResponseEntity<ApiResponse<?>> cancelAllocation(
            @PathVariable UUID allocationId,
            @RequestParam String cancelledBy) {
        return allocationService.cancelAllocation(allocationId, cancelledBy);
    }

    @GetMapping("/resource/{resourceId}")
    @PreAuthorize("hasAnyRole('RESOURCE-MANAGER', 'PROJECT-MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<?>> getAllocationsByResource(
            @PathVariable Long resourceId) {
        return allocationService.getAllocationsByResource(resourceId);
    }

    @GetMapping("/demand/{demandId}")
    @PreAuthorize("hasAnyRole('RESOURCE-MANAGER', 'DELIVERY-MANAGER', 'MANAGER')")
    public ResponseEntity<ApiResponse<?>> getAllocationsByDemand(
            @PathVariable UUID demandId) {
        return allocationService.getAllocationsByDemand(demandId);
    }

    @GetMapping("/project/{projectId}")
    @PreAuthorize("hasAnyRole('RESOURCE-MANAGER', 'PROJECT-MANAGER', 'ADMIN', 'DELIVERY-MANAGER')")
    public ResponseEntity<ApiResponse<?>> getAllocationsByProject(
            @PathVariable Long projectId) {
        return allocationService.getAllocationsByProject(projectId);
    }

    @GetMapping("/get-all-resources/{projectId}")
    @PreAuthorize("hasAnyRole('RESOURCE-MANAGER', 'ADMIN', 'PROJECT-MANAGER','HR-MANAGER')")
    public ResponseEntity<?> getAllResources(@PathVariable Long projectId) {
        return allocationService.getProjectResources(projectId);
    }

    @GetMapping("/overrides")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<?>> getOverrideAllocations() {
        return allocationService.getOverrideAllocations();
    }

    @PostMapping("/{allocationId}/close")
    @PreAuthorize("hasAnyRole('RESOURCE-MANAGER','PROJECT-MANAGER')")
    public ResponseEntity<ApiResponse<?>> closeAllocation(
            @PathVariable UUID allocationId,
            @RequestBody CloseAllocationDTO request) {
        return allocationService.closeAllocation(allocationId, request);
    }
    @GetMapping("/approvals/pending")
    @PreAuthorize("hasRole('DELIVERY-MANAGER')")
    public ResponseEntity<ApiResponse<?>> getPendingApprovals() {
        return allocationService.getPendingApprovals();
    }
    @PostMapping("/approvals/{allocationId}/approve")
    @PreAuthorize("hasRole('DELIVERY-MANAGER')")
    public ResponseEntity<ApiResponse<?>> approve(
            @PathVariable UUID allocationId,
            @CurrentUser UserDTO user) {

        return allocationService.approveAllocation(allocationId, user.getName());
    }
    @PostMapping("/approvals/{allocationId}/reject")
    @PreAuthorize("hasRole('DELIVERY-MANAGER')")
    public ResponseEntity<ApiResponse<?>> reject(
            @PathVariable UUID allocationId,
            @RequestBody InternalPoolAllocationApproval dto,
            @CurrentUser UserDTO user) {

        return allocationService.rejectAllocation(allocationId, dto.getRejectionReason(), user.getName());
    }

}
