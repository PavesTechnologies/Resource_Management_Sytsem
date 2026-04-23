package com.service_interface.allocation_service_interface;

import com.dto.allocation_dto.*;
import com.dto.centralised_dto.ApiResponse;
import com.entity.allocation_entities.ResourceAllocation;
import org.springframework.http.ResponseEntity;

import java.util.UUID;
import java.util.List;

public interface AllocationService {
    
    ResponseEntity<ApiResponse<?>> assignAllocation(AllocationRequestDTO allocationRequest);
    
    ResponseEntity<ApiResponse<?>> getAllocationById(UUID allocationId);
    
    ResponseEntity<ApiResponse<?>> updateAllocation(UUID allocationId, AllocationRequestDTO allocationRequest);
    
    ResponseEntity<ApiResponse<?>> cancelAllocation(UUID allocationId, String cancelledBy);
    
    ResponseEntity<ApiResponse<?>> getAllocationsByResource(Long resourceId);
    
    ResponseEntity<ApiResponse<?>> getAllocationsByDemand(UUID demandId);
    
    ResponseEntity<ApiResponse<?>> getAllocationsByProject(Long projectId);

    ResponseEntity<?> getProjectResources(Long projectId);

    ResponseEntity<ApiResponse<?>> getOverrideAllocations();

    ResponseEntity<ApiResponse<?>> closeAllocation(UUID allocationId, CloseAllocationDTO request);
    
    /**
     * Performs comprehensive skill gap analysis between demand and resource
     * Integrated skill gap matching engine for allocation decision support
     */
    ResponseEntity<ApiResponse<?>> analyzeSkillGap(SkillGapAnalysisRequestDTO request);
    
    
    // ==================== CONFLICT DETECTION METHODS ====================
    
    /**
     * Detects priority conflicts for a new allocation request
     */
    ConflictDetectionResult detectPriorityConflicts(AllocationRequestDTO allocationRequest);
    
    /**
     * Detects priority conflicts for a resource's allocations
     */
    List<AllocationConflictDTO> detectAllocationConflicts(Long resourceId);
    
    /**
     * Resolves a conflict with the specified action
     */
    ResponseEntity<ApiResponse<?>> resolveAllocationConflict(UUID conflictId, ConflictResolutionDTO resolution);
    
    /**
     * Gets pending conflicts for a resource
     */
    List<AllocationConflictDTO> getPendingConflictsForResource(Long resourceId);
    
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
}
