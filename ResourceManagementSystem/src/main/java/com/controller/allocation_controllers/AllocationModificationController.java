package com.controller.allocation_controllers;

import com.dto.ApiResponse;
import com.dto.UserDTO;
import com.dto.allocation_dto.AllocationModificationResponseDTO;
import com.dto.allocation_dto.CreateAllocationModificationDTO;
import com.security.CurrentUser;
import com.service_interface.allocation_service_interface.AllocationModificationService;
import com.service_imple.allocation_service_impl.AllocationModificationServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Allocation Modification Controller - Updated with Unified Allocation Change System
 * 
 * This controller now uses the unified allocation change system that eliminates redundancy
 * between modifications and overrides. ALL changes require Resource Manager approval.
 */
@RestController
@RequestMapping("/api/allocation-modifications")
@CrossOrigin
public class AllocationModificationController {

    @Autowired
    private AllocationModificationService allocationModificationService;
    
    @Autowired
    private AllocationModificationServiceImpl unifiedModificationService;

    /**
     * Create allocation change request - Enhanced CreateAllocationModificationDTO with override duration control
     * 
     * This endpoint handles both normal modifications and overrides automatically.
     * The system determines if an override is needed based on capacity constraints.
     * ALL changes require Resource Manager approval.
     * 
     * Examples:
     * 
     * 1️⃣ Standard Modification (≤100% total allocation):
     * POST /api/allocation-modifications/pm
     * {
     *   "allocationId": "uuid",
     *   "requestedAllocationPercentage": 80,
     *   "effectiveDate": "2026-03-27",
     *   "reason": "Project timeline adjustment - reduced scope"
     * }
     * 
     * Note: overrideEndDate is NOT required when total allocation ≤ 100%
     * 
     * 2️⃣ Override with Expiration Date (Required for >100% total allocation):
     * POST /api/allocation-modifications/pm
     * {
     *   "allocationId": "uuid",
     *   "requestedAllocationPercentage": 130,
     *   "effectiveDate": "2026-03-27",
     *   "overrideEndDate": "2026-05-31",
     *   "reason": "Critical project deadline - need extra capacity for 2 months"
     * }
     * 
     * Note: overrideEndDate is MANDATORY when total allocation (existing + requested) > 100%
     * 
     * 4️⃣ Role-Off (0% Allocation):
     * POST /api/allocation-modifications/pm
     * {
     *   "allocationId": "uuid",
     *   "requestedAllocationPercentage": 0,
     *   "effectiveDate": "2026-03-27",
     *   "reason": "Project completed - resource role-off"
     * }
     */
    @PostMapping("/pm")
    @PreAuthorize("hasRole('PROJECT-MANAGER')")
    public ResponseEntity<ApiResponse<?>> createModification(
            @RequestBody CreateAllocationModificationDTO dto,
            @CurrentUser UserDTO userDTO) {
        
        // Set requestedBy from current user if not provided
        if (dto.getRequestedBy() == null) {
            dto.setRequestedBy(userDTO.getName());
        }
        
        // Use the unified service directly with the enhanced CreateAllocationModificationDTO
        return unifiedModificationService.createUnifiedAllocationChangeFromDTO(dto, userDTO);
    }

    /**
     * RM Approval/Rejection - Unified endpoint
     * 
     * Examples:
     * PUT /api/allocation-modifications/{id}/rm/decision
     * {
     *   "decision": "APPROVE",
     *   "comments": "Approved due to project priority"
     * }
     * 
     * PUT /api/allocation-modifications/{id}/rm/decision
     * {
     *   "decision": "REJECT",
     *   "comments": "Rejected - resource already at capacity"
     * }
     */
    @PutMapping("/{id}/rm/decision")
    @PreAuthorize("hasRole('RESOURCE-MANAGER')")
    public ResponseEntity<ApiResponse<?>> processModificationDecision(
            @PathVariable UUID id,
            @RequestBody UnifiedDecisionDTO decisionDTO,
            @CurrentUser UserDTO userDTO) {
        
        return unifiedModificationService.processRMApproval(
                id, 
                decisionDTO.getDecision(), 
                decisionDTO.getComments(),
                userDTO
        );
    }

    /**
     * Get pending approvals for Resource Manager
     */
    @GetMapping("/rm/pending-approvals")
    @PreAuthorize("hasRole('RESOURCE-MANAGER')")
    public ResponseEntity<ApiResponse<?>> getPendingApprovals(@CurrentUser UserDTO rmUser) {
        return unifiedModificationService.getPendingApprovals(rmUser);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PROJECT-MANAGER', 'RESOURCE-MANAGER')")
    public ResponseEntity<ApiResponse<AllocationModificationResponseDTO>> getModificationById(
            @PathVariable UUID id) {
        return allocationModificationService.getModificationById(id);
    }

    @GetMapping("/demand/{demandId}")
    @PreAuthorize("hasAnyRole('PROJECT-MANAGER', 'RESOURCE-MANAGER')")
    public ResponseEntity<ApiResponse<List<AllocationModificationResponseDTO>>> getModificationsByDemand(
            @PathVariable UUID demandId) {
        return allocationModificationService.getModificationsByDemand(demandId);
    }

    @DeleteMapping("/{id}/pm")
    @PreAuthorize("hasRole('PROJECT-MANAGER')")
    public ResponseEntity<ApiResponse<?>> deleteModification(
            @PathVariable UUID id,
            @CurrentUser UserDTO userDTO) {
        return allocationModificationService.deleteModification(id, userDTO);
    }

    /**
     * DTO for unified decision making
     */
    public static class UnifiedDecisionDTO {
        private String decision; // "APPROVE" or "REJECT"
        private String comments;

        public String getDecision() { return decision; }
        public void setDecision(String decision) { this.decision = decision; }
        public String getComments() { return comments; }
        public void setComments(String comments) { this.comments = comments; }
    }
}
