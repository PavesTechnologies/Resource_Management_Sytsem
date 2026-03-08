package com.service_interface.allocation_service_interface;

import com.dto.allocation_dto.AllocationRequestDTO;
import com.dto.allocation_dto.SkillGapAnalysisRequestDTO;
import com.dto.allocation_dto.SkillGapAnalysisResponseDTO;
import com.dto.allocation_dto.ConflictDetectionResult;
import com.dto.allocation_dto.AllocationConflictDTO;
import com.dto.allocation_dto.ConflictResolutionDTO;
import com.dto.ApiResponse;
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
    
    /**
     * Performs comprehensive skill gap analysis between demand and resource
     * Integrated skill gap matching engine for allocation decision support
     */
    ResponseEntity<ApiResponse<?>> analyzeSkillGap(SkillGapAnalysisRequestDTO request);
    
    /**
     * Clear all skill-related caches
     * Call this when any skill reference data is updated
     */
    void clearAllSkillCaches();
    
    /**
     * Clear proficiency levels cache
     * Call this when proficiency levels are updated
     */
    void clearProficiencyLevelsCache();
    
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
}
