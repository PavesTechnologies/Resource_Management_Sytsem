package com.service_imple.allocation_service_imple;

import com.dto.allocation_dto.*;
import com.dto.ApiResponse;
import com.dto.resource.ResourceNameDTO;
import com.entity.allocation_entities.ResourceAllocation;
import com.entity.availability_entities.ResourceAvailabilityLedger;
import com.entity.demand_entities.Demand;
import com.entity_enums.allocation_enums.AllocationStatus;
import com.entity_enums.demand_enums.DemandStatus;
import com.global_exception_handler.ProjectExceptionHandler;
import com.repo.allocation_repo.AllocationRepository;
import com.repo.resource_repo.ResourceRepository;
import com.repo.demand_repo.DemandRepository;
import com.repo.availability_repo.ResourceAvailabilityLedgerRepository;
import com.service_interface.allocation_service_interface.AllocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Clean Allocation Service Implementation following SOLID principles
 * 
 * This service orchestrates the allocation process by delegating specific responsibilities
 * to specialized services:
 * - AllocationValidationService: All validation logic
 * - AllocationCapacityService: Timeline capacity validation
 * - AllocationConflictService: Conflict detection and resolution
 * - SkillGapAnalysisService: Skill compliance validation
 * - AvailabilityLedgerAsyncService: Async ledger updates
 */
@Service
@RequiredArgsConstructor
public class AllocationServiceImple implements AllocationService {

    private final AllocationRepository allocationRepository;
    private final ResourceRepository resourceRepository;
    private final DemandRepository demandRepository;
    private final ResourceAvailabilityLedgerRepository ledgerRepository;
    private final AllocationValidationService validationService;
    private final AllocationConflictService conflictService;
    private final SkillGapAnalysisService skillGapService;
    private final AvailabilityLedgerAsyncService ledgerAsyncService;

    /**
     * Main allocation method following clean architecture
     * 
     * Allocation Flow:
     * 1. validateRequest() - Basic request validation
     * 2. validateDemandOrProject() - Demand/project validation
     * 3. preloadAllocationData() - Batch data loading
     * 4. validateResourcesInParallel() - Parallel resource validation
     * 5. persistAllocations() - Save allocations
     * 6. triggerAsyncLedgerUpdate() - Async ledger updates
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> assignAllocation(AllocationRequestDTO allocationRequest) {
        try {
            // SECTION 1 — Request Validation
            validationService.validateRequest(allocationRequest);

            // SECTION 2 — Demand/Project Validation
            DemandProjectData demandProjectData = validationService.validateDemandOrProject(allocationRequest);

            // SECTION 3 — Data Preloading
            AllocationPreloadedData preloadedData = validationService.preloadAllocationData(
                    allocationRequest, demandProjectData.getDemand());

            // SECTION 4 — Parallel Resource Validation
            AllocationValidationResult validationResult = validationService.validateResourcesInParallel(
                    allocationRequest, demandProjectData, preloadedData);

            // SECTION 5 — Persist Valid Allocations
            List<ResourceAllocation> savedAllocations = persistAllocations(validationResult.getValidAllocations());

            // SECTION 5.1 — Check Demand Fulfillment
            if (allocationRequest.getDemandId() != null) {
                checkAndUpdateDemandFulfillment(allocationRequest.getDemandId());
            }

            // SECTION 6 — Async Ledger Updates
            triggerAsyncLedgerUpdate(savedAllocations);

            // Build response
            return buildAllocationResponse(savedAllocations, validationResult.getFailures());

        } catch (ProjectExceptionHandler e) {
            return ResponseEntity.badRequest().body(
                    new ApiResponse<>(false, e.getMessage(), null)
            );
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                    new ApiResponse<>(false, "Error creating allocation: " + e.getMessage(), null)
            );
        }
    }

    @Override
    public ResponseEntity<ApiResponse<?>> getAllocationById(UUID allocationId) {
        try {
            Optional<ResourceAllocation> allocation = allocationRepository.findById(allocationId);
            if (allocation.isPresent()) {
                AllocationResponseDTO response = mapToResponseDTO(allocation.get());
                return ResponseEntity.ok(
                        new ApiResponse<>(true, "Allocation found", response)
                );
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                new ApiResponse<>(false, "Error retrieving allocation: " + e.getMessage(), null)
            );
        }
    }

    @Override
    public ResponseEntity<ApiResponse<?>> updateAllocation(UUID allocationId, AllocationRequestDTO allocationRequest) {
        try {
            Optional<ResourceAllocation> existingAllocation = allocationRepository.findById(allocationId);
            
            if (!existingAllocation.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            ResourceAllocation allocation = existingAllocation.get();
            if (allocation.getAllocationStatus() == AllocationStatus.ENDED ||
                    allocation.getAllocationStatus() == AllocationStatus.CANCELLED) {

                return ResponseEntity.badRequest().body(
                        new ApiResponse<>(false,
                                "Closed or cancelled allocations cannot be modified", null)
                );
            }

            // Update fields
            allocation.setAllocationStartDate(allocationRequest.getAllocationStartDate());
            allocation.setAllocationEndDate(allocationRequest.getAllocationEndDate());
            allocation.setAllocationPercentage(allocationRequest.getAllocationPercentage());
            allocation.setAllocationStatus(allocationRequest.getAllocationStatus());

            // If allocation is ended, enforce role-off reason
            if (allocationRequest.getAllocationStatus() == AllocationStatus.ENDED) {
                if (allocationRequest.getRoleOffReason() == null) {
                    throw new ProjectExceptionHandler(HttpStatus.BAD_REQUEST, "ROLE_OFF_REASON_REQUIRED", "Role-off reason is required when ending an allocation.");
                }
                if (allocationRequest.getRoleOffDate() == null) {
                    throw new ProjectExceptionHandler(HttpStatus.BAD_REQUEST, "ROLE_OFF_DATE_REQUIRED", "Role-off date is required when ending an allocation.");
                }
                allocation.setRoleOffReason(allocationRequest.getRoleOffReason());
                allocation.setRoleOffDate(allocationRequest.getRoleOffDate());
            }

            // Validate capacity for update
            validateResourceCapacityForUpdate(allocationId, allocationRequest);

            ResourceAllocation updatedAllocation = allocationRepository.save(allocation);
            
            // Update availability ledger for the modified allocation period
            updateAvailabilityLedgerForAllocation(updatedAllocation);

            // Check demand fulfillment if allocation is associated with a demand
            if (updatedAllocation.getDemand() != null) {
                checkAndUpdateDemandFulfillment(updatedAllocation.getDemand().getDemandId());
            }

            AllocationResponseDTO response = mapToResponseDTO(updatedAllocation);
            return ResponseEntity.ok(
                    new ApiResponse<>(true, "Allocation updated successfully", response)
            );

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                new ApiResponse<>(false, "Error updating allocation: " + e.getMessage(), null)
            );
        }
    }

    @Override
    public ResponseEntity<ApiResponse<?>> cancelAllocation(UUID allocationId, String cancelledBy) {
        try {
            Optional<ResourceAllocation> existingAllocation = allocationRepository.findById(allocationId);
            
            if (!existingAllocation.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            ResourceAllocation allocation = existingAllocation.get();
            allocation.setAllocationStatus(AllocationStatus.CANCELLED);
            
            ResourceAllocation cancelledAllocation = allocationRepository.save(allocation);
            
            // Update availability ledger to reflect cancellation
            updateAvailabilityLedgerForAllocation(cancelledAllocation);

            // Check demand fulfillment if allocation is associated with a demand
            // (might need to revert FULFILLED status if no longer fully allocated)
            if (cancelledAllocation.getDemand() != null) {
                checkAndUpdateDemandFulfillment(cancelledAllocation.getDemand().getDemandId());
            }

            AllocationResponseDTO response = mapToResponseDTO(cancelledAllocation);
            return ResponseEntity.ok(
                    new ApiResponse<>(true, "Allocation cancelled successfully", response)
            );

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                new ApiResponse<>(false, "Error cancelling allocation: " + e.getMessage(), null)
            );
        }
    }

    @Override
    public ResponseEntity<ApiResponse<?>> getAllocationsByResource(Long resourceId) {
        try {
            List<ResourceAllocation> allocations = allocationRepository.findByResource_ResourceId(resourceId);
            List<AllocationResponseDTO> response = allocations.stream()
                    .map(this::mapToResponseDTO)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(
                new ApiResponse<>(true, "Allocations retrieved successfully", response)
            );

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                new ApiResponse<>(false, "Error retrieving allocations: " + e.getMessage(), null)
            );
        }
    }

    @Override
    public ResponseEntity<ApiResponse<?>> getOverrideAllocations() {

        List<ResourceAllocation> overrides = allocationRepository.findByOverrideFlagTrue();

        return ResponseEntity.ok(
                ApiResponse.success("Override allocations retrieved", overrides)
        );
    }

    @Override
    public ResponseEntity<ApiResponse<?>> getAllocationsByDemand(UUID demandId) {
        try {
            List<ResourceAllocation> allocations = allocationRepository.findByDemand_DemandId(demandId);
            List<AllocationResponseDTO> response = allocations.stream()
                    .map(this::mapToResponseDTO)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(
                new ApiResponse<>(true, "Allocations retrieved successfully", response)
            );

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                new ApiResponse<>(false, "Error retrieving allocations: " + e.getMessage(), null)
            );
        }
    }

    @Override
    public ResponseEntity<ApiResponse<?>> getAllocationsByProject(Long projectId) {
        try {
            List<ResourceAllocation> allocations = allocationRepository.findByProject_PmsProjectId(projectId);
            List<AllocationResponseDTO> response = allocations.stream()
                    .map(this::mapToResponseDTO)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(
                new ApiResponse<>(true, "Allocations retrieved successfully", response)
            );

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                new ApiResponse<>(false, "Error retrieving allocations: " + e.getMessage(), null)
            );
        }
    }

    @Override
    public ResponseEntity<ApiResponse<?>> analyzeSkillGap(SkillGapAnalysisRequestDTO request) {
        try {
            // Validate demand exists
            Demand demand = demandRepository.findById(request.getDemandId())
                .orElseThrow(() -> new ProjectExceptionHandler(
                    HttpStatus.NOT_FOUND,
                    "DEMAND_NOT_FOUND",
                    "Demand not found with ID: " + request.getDemandId()
                ));

            // Validate resource exists
            if (!resourceRepository.existsById(request.getResourceId())) {
                throw new ProjectExceptionHandler(
                    HttpStatus.NOT_FOUND,
                    "RESOURCE_NOT_FOUND",
                    "Resource not found with ID: " + request.getResourceId()
                );
            }

            // Perform comprehensive skill gap analysis
            SkillGapAnalysisResponseDTO analysis = skillGapService.performSkillGapAnalysis(demand, request.getResourceId());

            return ResponseEntity.ok(
                new ApiResponse<>(true, "Skill gap analysis completed successfully", analysis)
            );

        } catch (ProjectExceptionHandler e) {
            return ResponseEntity.badRequest().body(
                new ApiResponse<>(false, e.getMessage(), null)
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ApiResponse<>(false, "Failed to analyze skill gap: " + e.getMessage(), null)
            );
        }
    }

    @Override
    public ConflictDetectionResult detectPriorityConflicts(AllocationRequestDTO allocationRequest) {
        return conflictService.detectPriorityConflicts(allocationRequest, allocationRequest.getResourceId().get(0));
    }

    @Override
    public List<AllocationConflictDTO> detectAllocationConflicts(Long resourceId) {
        return conflictService.detectAllocationConflicts(resourceId);
    }

    @Override
    public ResponseEntity<ApiResponse<?>> resolveAllocationConflict(UUID conflictId, ConflictResolutionDTO resolution) {
        return conflictService.resolveAllocationConflict(conflictId, resolution);
    }

    @Override
    public List<AllocationConflictDTO> getPendingConflictsForResource(Long resourceId) {
        return conflictService.getPendingConflictsForResource(resourceId);
    }

    @Override
    public List<AllocationConflictDTO> getAllPendingConflicts() {
        return conflictService.getAllPendingConflicts();
    }

    @Override
    public void clearAllSkillCaches() {
        skillGapService.clearAllSkillCaches();
    }

    @Override
    public void clearProficiencyLevelsCache() {
        skillGapService.clearProficiencyLevelsCache();
    }

    @Override
    public ResponseEntity<?> getProjectResources(Long projectId) {
        List<ResourceNameDTO> resources = allocationRepository.findResourcesByProjectId(projectId)
                .stream()
                .map(resource -> new ResourceNameDTO(resource.getFullName(), resource.getResourceId(), resource.getDesignation()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(new ApiResponse<>(true, "Resources by Project Id", resources));
    }

    // ==================== PRIVATE HELPER METHODS ====================

    /**
     * Persists valid allocations to database
     */
    private List<ResourceAllocation> persistAllocations(List<ResourceAllocation> validAllocations) {
        List<ResourceAllocation> savedAllocations = allocationRepository.saveAll(validAllocations);
        
        // Update the input allocations with the saved IDs and return them
        // This preserves the fully loaded resource data from preloading
        for (int i = 0; i < savedAllocations.size(); i++) {
            validAllocations.get(i).setAllocationId(savedAllocations.get(i).getAllocationId());
        }
        
        return validAllocations;
    }

    /**
     * Triggers async ledger updates for saved allocations
     */
    private void triggerAsyncLedgerUpdate(List<ResourceAllocation> savedAllocations) {
        for (ResourceAllocation allocation : savedAllocations) {
            ledgerAsyncService.updateLedgerAsync(allocation);
        }
    }

    /**
     * Builds allocation response with success/failure counts
     */
    private ResponseEntity<ApiResponse<?>> buildAllocationResponse(
            List<ResourceAllocation> savedAllocations, 
            List<AllocationFailure> failures) {
        
        int successCount = savedAllocations.size();
        int failureCount = failures.size();

        String message;
        if (failureCount == 0) {
            message = "Allocation Successful";
        } else if (successCount == 0) {
            message = "Allocation Failed";
        } else {
            message = "Allocation Partially Successful";
        }

        // Fetch saved allocations with relationships to avoid lazy loading issues
        List<UUID> allocationIds = savedAllocations.stream()
                .map(ResourceAllocation::getAllocationId)
                .collect(Collectors.toList());
        
        List<ResourceAllocation> allocationsWithRelations = allocationRepository.findByAllocationIdIn(allocationIds);

        Map<String, Object> response = new HashMap<>();
        response.put("successCount", successCount);
        response.put("failureCount", failureCount);
        response.put("savedAllocations", allocationsWithRelations.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList()));
        response.put("failedResources", failures);

        return ResponseEntity.ok(
                new ApiResponse<>(true, message, response)
        );
    }

    /**
     * Validates resource capacity for updates
     */
    private void validateResourceCapacityForUpdate(UUID allocationId, AllocationRequestDTO request) {
        List<ResourceAllocation> conflictingAllocations =
                allocationRepository.findConflictingAllocationsForResources(
                        request.getResourceId(),
                        request.getAllocationStartDate(),
                        request.getAllocationEndDate()
                );

        int existingAllocation = conflictingAllocations.stream()
                .filter(a -> !a.getAllocationId().equals(allocationId))
                .mapToInt(ResourceAllocation::getAllocationPercentage)
                .sum();

        int newAllocation = request.getAllocationPercentage();
        int total = existingAllocation + newAllocation;

        if (total > 100) {
            throw new ProjectExceptionHandler(
                    HttpStatus.BAD_REQUEST,
                    "OVER_ALLOCATION",
                    "Updating allocation exceeds resource capacity"
            );
        }
    }

    @Transactional
    public ResponseEntity<ApiResponse<?>> closeAllocation(UUID allocationId, CloseAllocationDTO request) {

        Optional<ResourceAllocation> allocationOpt = allocationRepository.findById(allocationId);

        if (allocationOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ResourceAllocation allocation = allocationOpt.get();

        if (allocation.getAllocationStatus() == AllocationStatus.ENDED) {
            return ResponseEntity.badRequest().body(
                    new ApiResponse<>(false, "Allocation already ended", null)
            );
        }

        LocalDate closureDate = request.getClosureDate();

        if (closureDate.isBefore(allocation.getAllocationStartDate())) {
            return ResponseEntity.badRequest().body(
                    new ApiResponse<>(false, "Closure date cannot be before start date", null)
            );
        }

        allocation.setAllocationEndDate(closureDate);
        allocation.setAllocationStatus(AllocationStatus.ENDED);

        ResourceAllocation saved = allocationRepository.save(allocation);

        updateAvailabilityLedgerForAllocation(saved);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Allocation closed successfully", mapToResponseDTO(saved))
        );
    }

    /**
     * Updates the ResourceAvailabilityLedger for the given allocation
     */
    public void updateAvailabilityLedgerForAllocation(ResourceAllocation allocation) {
        try {
            Long resourceId = allocation.getResource().getResourceId();
            LocalDate startDate = allocation.getAllocationStartDate();
            LocalDate endDate = allocation.getAllocationEndDate();
            
            // For each day in the allocation period, update the ledger
            LocalDate currentDate = startDate;
            while (!currentDate.isAfter(endDate)) {
                updateLedgerForDate(resourceId, currentDate);
                currentDate = currentDate.plusDays(1);
            }
                                
        } catch (Exception e) {
            // Don't throw here to avoid failing the main allocation operation
        }
    }

    /**
     * Updates the ledger for a specific resource and date
     */
    private void updateLedgerForDate(Long resourceId, LocalDate date) {
        try {
            // Calculate total allocation percentage for this resource on this date
            List<ResourceAllocation> activeAllocations = allocationRepository
                .findActiveAllocationsForResourceOnDate(resourceId, date);
            
            int totalAllocation = activeAllocations.stream()
                .mapToInt(ResourceAllocation::getAllocationPercentage)
                .sum();
            
            // Find existing ledger entry
            Optional<ResourceAvailabilityLedger> existingLedger = 
                ledgerRepository.findByResourceIdAndDate(resourceId, date);
            
            ResourceAvailabilityLedger ledger;
            if (existingLedger.isPresent()) {
                ledger = existingLedger.get();
                ledger.setTotalAllocation(totalAllocation);
                ledger.setAvailablePercentage(Math.max(0, 100 - totalAllocation));
                ledger.setAvailabilityTrustFlag(true);
                ledger.setLastCalculatedAt(LocalDateTime.now());
            } else {
                // Create new ledger entry
                ledger = new ResourceAvailabilityLedger();
                ledger.setResource(resourceRepository.findById(resourceId).orElse(null));
                ledger.setPeriodStart(date);
                ledger.setPeriodEnd(date); // Daily ledger
                ledger.setTotalAllocation(totalAllocation);
                ledger.setAvailablePercentage(Math.max(0, 100 - totalAllocation));
                ledger.setAvailabilityTrustFlag(true);
                ledger.setLastCalculatedAt(LocalDateTime.now());
            }
            
            ledgerRepository.save(ledger);
            
        } catch (Exception e) {
            // Error handling without logs
        }
    }

    /**
     * Checks if demand is fully allocated and updates status to FULFILLED if true
     * Reverts to APPROVED if no longer fully allocated
     * Validates both resource count and percentage matching
     */
    private void checkAndUpdateDemandFulfillment(UUID demandId) {
        try {
            Demand demand = demandRepository.findById(demandId).orElse(null);
            if (demand == null) {
                return;
            }

            // Count active allocations for this demand
            List<ResourceAllocation> activeAllocations = allocationRepository.findByDemand_DemandId(demandId)
                    .stream()
                    .filter(alloc -> alloc.getAllocationStatus() == AllocationStatus.ACTIVE)
                    .toList();

            int allocatedResources = activeAllocations.size();
            
            // Check if allocated resources match required resources
            if (allocatedResources >= demand.getResourcesRequired()) {
                // Additional validation: Check if all allocations match demand percentage
                boolean allPercentagesMatch = activeAllocations.stream()
                        .allMatch(alloc -> alloc.getAllocationPercentage().equals(demand.getAllocationPercentage()));
                
                // Update to FULFILLED if not already FULFILLED and percentages match
                if (demand.getDemandStatus() != DemandStatus.FULFILLED && allPercentagesMatch) {
                    demand.setDemandStatus(DemandStatus.FULFILLED);
                    demandRepository.save(demand);
                }
            } else {
                // Revert to APPROVED if currently FULFILLED but no longer fully allocated
                if (demand.getDemandStatus() == DemandStatus.FULFILLED) {
                    demand.setDemandStatus(DemandStatus.APPROVED);
                    demandRepository.save(demand);
                }
            }
        } catch (Exception e) {
            // Error handling without logs
        }
    }

    /**
     * Maps ResourceAllocation to AllocationResponseDTO
     */
    private AllocationResponseDTO mapToResponseDTO(ResourceAllocation allocation) {
        AllocationResponseDTO dto = new AllocationResponseDTO();

        dto.setAllocationId(allocation.getAllocationId());

        if (allocation.getResource() != null) {
            dto.setFullName(allocation.getResource().getFullName());
            dto.setEmail(allocation.getResource().getEmail());
        } else {
            dto.setFullName("Unknown Resource");
            dto.setEmail("unknown@example.com");
        }

        if (allocation.getDemand() != null) {
            dto.setDemandName(allocation.getDemand().getDemandName());
        } else {
            dto.setDemandName("Unknown Demand");
        }

        dto.setAllocationStartDate(allocation.getAllocationStartDate());
        dto.setAllocationEndDate(allocation.getAllocationEndDate());
        dto.setAllocationPercentage(allocation.getAllocationPercentage());
        dto.setAllocationStatus(allocation.getAllocationStatus().name());
        dto.setCreatedBy(allocation.getCreatedBy());

        return dto;
    }
}
