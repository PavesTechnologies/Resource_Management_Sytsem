package com.controller.allocation_controllers;

import com.dto.centralised_dto.ApiResponse;
import com.dto.centralised_dto.UserDTO;
import com.dto.allocation_dto.AllocationModificationResponseDTO;
import com.dto.allocation_dto.CreateAllocationModificationDTO;
import com.security.CurrentUser;
import com.service_interface.allocation_service_interface.AllocationModificationService;
import com.service_imple.allocation_service_imple.AllocationModificationServiceImpl;
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
public class AllocationModificationController {

    @Autowired
    private AllocationModificationService allocationModificationService;
    
    @Autowired
    private AllocationModificationServiceImpl unifiedModificationService;

    @PostMapping("/pm")
    @PreAuthorize("hasRole('Project_Manager')")
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
    @PreAuthorize("hasRole('Resource_Manager')")
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
    @PreAuthorize("hasRole('Resource_Manager')")
    public ResponseEntity<ApiResponse<?>> getPendingApprovals(@CurrentUser UserDTO rmUser) {
        return unifiedModificationService.getPendingApprovals(rmUser);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('Project_Manager', 'Resource_Manager')")
    public ResponseEntity<ApiResponse<AllocationModificationResponseDTO>> getModificationById(
            @PathVariable UUID id) {
        return allocationModificationService.getModificationById(id);
    }

    @GetMapping("/demand/{demandId}")
    @PreAuthorize("hasAnyRole('Project_Manager', 'Resource_Manager')")
    public ResponseEntity<ApiResponse<List<AllocationModificationResponseDTO>>> getModificationsByDemand(
            @PathVariable UUID demandId) {
        return allocationModificationService.getModificationsByDemand(demandId);
    }

    @DeleteMapping("/{id}/pm")
    @PreAuthorize("hasRole('Project_Manager')")
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
