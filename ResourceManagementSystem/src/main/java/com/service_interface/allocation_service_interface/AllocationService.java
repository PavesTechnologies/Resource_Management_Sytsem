package com.service_interface.allocation_service_interface;

import com.dto.allocation_dto.*;
import com.dto.centralised_dto.ApiResponse;
import com.dto.centralised_dto.UserDTO;
import com.entity.allocation_entities.ResourceAllocation;
import org.springframework.http.ResponseEntity;

import java.util.UUID;
import java.util.List;

public interface AllocationService {
    
    ResponseEntity<ApiResponse<?>> assignAllocation(AllocationRequestDTO allocationRequest);
    
    ResponseEntity<ApiResponse<?>> getAllocationById(UUID allocationId);
    
    ResponseEntity<ApiResponse<?>> updateAllocation(UUID allocationId, AllocationRequestDTO allocationRequest);
    
    ResponseEntity<ApiResponse<?>> cancelAllocation(UUID allocationId, String cancelledBy);
    
    ResponseEntity<ApiResponse<?>> getAllocationsByResource(String resourceId);
    
    ResponseEntity<ApiResponse<?>> getAllocationsByDemand(UUID demandId);
    
    ResponseEntity<ApiResponse<?>> getAllocationsByProject(Long projectId);

    ResponseEntity<ApiResponse<?>> getProjectResources(Long projectId);

    ResponseEntity<ApiResponse<?>> getOverrideAllocations();

    ResponseEntity<ApiResponse<?>> closeAllocation(UUID allocationId, CloseAllocationDTO request);
    
    /**
     * Performs comprehensive skill gap analysis between demand and resource
     * Integrated skill gap matching engine for allocation decision support
     */
    ResponseEntity<ApiResponse<?>> analyzeSkillGap(SkillGapAnalysisRequestDTO request);

    /**
     * Quick allocate bench resource to demand
     */
    ResponseEntity<ApiResponse<?>> quickAllocateResource(String resourceId, UUID demandId, Integer allocationPercentage, UserDTO user);
    
    
    // ==================== CONFLICT DETECTION METHODS ====================
    
    /**
     * Detects priority conflicts for a new allocation request
     */
    ConflictDetectionResult detectPriorityConflicts(AllocationRequestDTO allocationRequest);
    
    /**
     * Detects priority conflicts for a resource's allocations
     */
    List<AllocationConflictDTO> detectAllocationConflicts(String resourceId);
    
    /**
     * Resolves a conflict with the specified action
     */
    ResponseEntity<ApiResponse<?>> resolveAllocationConflict(UUID conflictId, ConflictResolutionDTO resolution);
    
    /**
     * Gets pending conflicts for a resource
     */
    List<AllocationConflictDTO> getPendingConflictsForResource(String resourceId);
    
    /**
     * Gets all pending conflicts
     */
    List<AllocationConflictDTO> getAllPendingConflicts();
    
    /**
     * Updates the ResourceAvailabilityLedger for the given allocation
     */
    void updateAvailabilityLedgerForAllocation(ResourceAllocation allocation);
    
    /**
     * Approves a pending allocation
     */
    ResponseEntity<ApiResponse<?>> approveAllocation(UUID allocationId, String dmName);
    ResponseEntity<ApiResponse<?>> rejectAllocation(UUID allocationId, String reason, String dmName);
    ResponseEntity<ApiResponse<?>> getPendingApprovals();

    /**
     * Checks whether a demand is still fulfilled after an allocation is closed/cancelled,
     * and reverts it to APPROVED if the fulfilling allocations are gone.
     */
    void checkAndUpdateDemandFulfillment(UUID demandId);
}
