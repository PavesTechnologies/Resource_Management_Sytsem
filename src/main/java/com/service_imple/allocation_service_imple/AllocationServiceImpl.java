package com.service_imple.allocation_service_imple;

import com.dto.allocation_dto.*;
import com.dto.centralised_dto.ApiResponse;
import com.dto.centralised_dto.UserDTO;
import com.dto.resource.ResourceNameDTO;
import com.entity.allocation_entities.AllocationModification;
import com.entity.allocation_entities.ResourceAllocation;
import com.entity.bench.ResourceState;
import com.entity.demand_entities.Demand;
import com.entity.demand_entities.DemandSLA;
import com.entity_enums.allocation_enums.AllocationStatus;
import com.entity_enums.allocation_enums.AllocationType;
import com.entity_enums.allocation_enums.ApprovalStatus;
import com.entity_enums.allocation_enums.AllocationModificationStatus;
import com.entity_enums.bench.StateType;
import com.entity_enums.bench.SubState;
import com.entity_enums.demand_enums.DemandStatus;
import com.global_exception_handler.AllocationExceptionHandler;
import com.repo.allocation_repo.AllocationRepository;
import com.repo.bench_repo.ResourceStateRepository;
import com.repo.demand_repo.DemandSLARepository;
import com.repo.allocation_repo.AllocationModificationRepository;
import com.repo.resource_repo.ResourceRepository;
import com.repo.demand_repo.DemandRepository;
import com.service_interface.allocation_service_interface.AllocationService;
import com.service_interface.ledger_service_interface.LedgerAvailabilityCalculationService;
import com.service_imple.skill_service_impl.ResourceSkillUsageService;
import com.service_imple.bench_service_impl.BenchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AllocationServiceImpl implements AllocationService {

    private final AllocationRepository allocationRepository;
    private final AllocationModificationRepository allocationModificationRepository;
    private final ResourceRepository resourceRepository;
    private final DemandRepository demandRepository;
    private final AllocationValidationService validationService;
    private final AllocationConflictService conflictService;
    private final SkillGapAnalysisService skillGapService;
    private final AvailabilityLedgerAsyncService ledgerAsyncService;
    private final DemandSLARepository demandSLARepository;
    private final LedgerAvailabilityCalculationService availabilityCalculationService;
    private final BenchService benchDetectionService;
    private final ResourceSkillUsageService resourceSkillUsageService;
    private final ResourceStateRepository resourceStateRepository;

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "active-allocations", allEntries = true),
        @CacheEvict(value = "dashboard-kpis", allEntries = true),
        @CacheEvict(value = "bench-resources", allEntries = true),
        @CacheEvict(value = "bench-matches", allEntries = true),
        @CacheEvict(value = "resource-timelines", allEntries = true),
        @CacheEvict(value = "demands", allEntries = true)
    })
    public ResponseEntity<ApiResponse<?>> assignAllocation(AllocationRequestDTO allocationRequest) {
        try {
            validationService.validateRequest(allocationRequest);
            DemandProjectData demandProjectData = validationService.validateDemandOrProject(allocationRequest);
            
            // Auto-populate allocation percentage from demand if not provided
            if (allocationRequest.getDemandId() != null && demandProjectData.getDemand() != null) {
                Demand demand = demandProjectData.getDemand();
                if (allocationRequest.getAllocationPercentage() == null) {
                    allocationRequest.setAllocationPercentage(demand.getAllocationPercentage());
                    log.info("Auto-populated allocation percentage from demand: {}%", demand.getAllocationPercentage());
                }

                // Validate that demand is approved by delivery manager before allowing allocation
                if (demand.getDemandStatus() != DemandStatus.APPROVED) {
                    throw new AllocationExceptionHandler(
                        HttpStatus.BAD_REQUEST,
                        "DEMAND_NOT_APPROVED",
                        "Cannot allocate resources to demand '" + demand.getDemandName() + "'. Demand must be approved by Delivery Manager before resource allocation. Current status: " + demand.getDemandStatus()
                    );
                }
            }
            
            // Validate and set allocation type logic
            validateAndSetAllocationType(allocationRequest);

            AllocationPreloadedData preloadedData = validationService.preloadAllocationData(allocationRequest, demandProjectData.getDemand());
            AllocationValidationResult validationResult = validationService.validateResourcesInParallel(allocationRequest, demandProjectData, preloadedData);

            List<ResourceAllocation> savedAllocations = persistAllocations(validationResult.getValidAllocations());

            if (allocationRequest.getDemandId() != null) {
                checkAndUpdateDemandFulfillment(allocationRequest.getDemandId());
            }

            triggerAsyncLedgerUpdate(savedAllocations);

            for (ResourceAllocation allocation : savedAllocations) {
                if (allocation.getAllocationStatus() == AllocationStatus.ACTIVE) {
                    benchDetectionService.moveToProject(allocation.getResource().getResourceId(), allocation.getAllocationId());
                }
            }

            return buildAllocationResponse(savedAllocations, validationResult.getFailures());

        } catch (AllocationExceptionHandler e) {
            ApiResponse<Object> response = new ApiResponse<>();
            return ResponseEntity.badRequest().body(response.getAPIResponse(false, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error creating allocation: {}", e.getMessage());
            ApiResponse<Object> response = new ApiResponse<>();
            return ResponseEntity.internalServerError().body(response.getAPIResponse(false, "Error creating allocation", null));
        }
    }

    @Override
    public ResponseEntity<ApiResponse<?>> getAllocationById(UUID allocationId) {
        try {
            Optional<ResourceAllocation> allocation = allocationRepository.findById(allocationId);
            if (allocation.isPresent()) {
                ApiResponse<Object> response = new ApiResponse<>();
                return ResponseEntity.ok(response.getAPIResponse(true, "Allocation found", mapToResponseDTO(allocation.get())));
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error retrieving allocation {}: {}", allocationId, e.getMessage());
            ApiResponse<Object> response = new ApiResponse<>();
            return ResponseEntity.internalServerError().body(response.getAPIResponse(false, "Error retrieving allocation", null));
        }
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "active-allocations", allEntries = true),
        @CacheEvict(value = "dashboard-kpis", allEntries = true),
        @CacheEvict(value = "bench-resources", allEntries = true),
        @CacheEvict(value = "bench-matches", allEntries = true),
        @CacheEvict(value = "resource-timelines", allEntries = true),
        @CacheEvict(value = "demands", allEntries = true)
    })
    public ResponseEntity<ApiResponse<?>> updateAllocation(UUID allocationId, AllocationRequestDTO allocationRequest) {
        try {
            Optional<ResourceAllocation> existingAllocation = allocationRepository.findById(allocationId);
            if (existingAllocation.isEmpty()) return ResponseEntity.notFound().build();

            ResourceAllocation allocation = existingAllocation.get();

            if (allocation.getAllocationStatus() == AllocationStatus.DELETED) {
                ApiResponse<Object> response = new ApiResponse<>();
                return ResponseEntity.badRequest().body(
                        response.getAPIResponse(false,
                                "Deleted allocations cannot be modified", null));
            }

            if (allocation.getAllocationStatus() == AllocationStatus.ENDED ||
                    allocation.getAllocationStatus() == AllocationStatus.CANCELLED) {

                ApiResponse<Object> response = new ApiResponse<>();

                return ResponseEntity.badRequest().body(
                        response.getAPIResponse(false,
                                "Closed or cancelled allocations cannot be modified", null));
            }

            if (allocationRequest.getAllocationStatus() == AllocationStatus.ENDED) {
                throw new AllocationExceptionHandler(HttpStatus.BAD_REQUEST, "USE_ROLE_OFF_FLOW",
                        "Use the role-off flow to end an allocation.");
            }

            // STRICT VALIDATION: If allocation is linked to a demand, allocation percentage cannot be changed
            if (allocation.getDemand() != null && allocationRequest.getAllocationPercentage() != null) {
                validationService.validateAllocationPercentageMatchesDemand(allocationRequest.getAllocationPercentage(), allocation.getDemand());
            }

            // Validate and set allocation type logic for updates
            validateAndSetAllocationType(allocationRequest);

            allocation.setAllocationStartDate(allocationRequest.getAllocationStartDate());
            allocation.setAllocationEndDate(allocationRequest.getAllocationEndDate());
            // Only update allocation percentage if it's provided and passes validation
            if (allocationRequest.getAllocationPercentage() != null) {
                allocation.setAllocationPercentage(allocationRequest.getAllocationPercentage());
            }
            allocation.setAllocationStatus(allocationRequest.getAllocationStatus());
            allocation.setAllocationType(allocationRequest.getAllocationType() != null ? allocationRequest.getAllocationType() : AllocationType.ACTIVE);
            allocation.setPlannedStartDate(allocationRequest.getPlannedStartDate());

            validateResourceCapacityForUpdate(allocationId, allocationRequest);
            ResourceAllocation updatedAllocation = allocationRepository.save(allocation);
            updateAvailabilityLedgerForAllocation(updatedAllocation);

            if (updatedAllocation.getAllocationStatus() == AllocationStatus.ENDED) {
                resourceSkillUsageService.updateResourceSkillLastUsedOnRoleOff(
                    updatedAllocation.getResource(), updatedAllocation.getProject(), 
                    allocationRequest.getRoleOffDate(), updatedAllocation.getAllocationEndDate(),
                    updatedAllocation.getProject() != null ? updatedAllocation.getProject().getEndDate().toLocalDate() : null
                );
            }

            if (updatedAllocation.getAllocationStatus() == AllocationStatus.ACTIVE) {
                benchDetectionService.moveToProject(updatedAllocation.getResource().getResourceId(), updatedAllocation.getAllocationId());
            } else if (updatedAllocation.getAllocationStatus() == AllocationStatus.ENDED || updatedAllocation.getAllocationStatus() == AllocationStatus.CANCELLED) {
                benchDetectionService.detectBenchResources();
            }

            if (updatedAllocation.getDemand() != null) {
                checkAndUpdateDemandFulfillment(updatedAllocation.getDemand().getDemandId());
            }

            ApiResponse<Object> response = new ApiResponse<>();
            return ResponseEntity.ok(response.getAPIResponse(true, "Allocation updated successfully", mapToResponseDTO(updatedAllocation)));
        } catch (Exception e) {
            log.error("Error updating allocation {}: {}", allocationId, e.getMessage());
            ApiResponse<Object> response = new ApiResponse<>();
            return ResponseEntity.internalServerError().body(response.getAPIResponse(false, "Error updating allocation", null));
        }
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "active-allocations", allEntries = true),
        @CacheEvict(value = "dashboard-kpis",     allEntries = true),
        @CacheEvict(value = "bench-resources",    allEntries = true),
        @CacheEvict(value = "bench-matches",      allEntries = true),
        @CacheEvict(value = "resource-timelines", allEntries = true),
        @CacheEvict(value = "demands",            allEntries = true)
    })
    public ResponseEntity<ApiResponse<?>> cancelAllocation(UUID allocationId, String cancelledBy) {
        try {
            Optional<ResourceAllocation> existingAllocation = allocationRepository.findById(allocationId);
            if (existingAllocation.isEmpty()) return ResponseEntity.notFound().build();

            ResourceAllocation allocation = existingAllocation.get();

            // Check if allocation is already cancelled or ended
            if (allocation.getAllocationStatus() == AllocationStatus.CANCELLED ||
                    allocation.getAllocationStatus() == AllocationStatus.ENDED ||
                    allocation.getAllocationStatus() == AllocationStatus.ROLLED_OFF ||
                    allocation.getAllocationStatus() == AllocationStatus.DELETED) {

                ApiResponse<Object> response = new ApiResponse<>();

                return ResponseEntity.badRequest().body(
                        response.getAPIResponse(false,
                                "Allocation is already closed or deleted", null));
            }

            // For PLANNED allocations, cancellation before activation should revert demand status
            boolean wasPlannedAllocation = allocation.getAllocationType() == AllocationType.PLANNED
                    && allocation.getAllocationStatus() == AllocationStatus.PLANNED;

            allocation.setAllocationStatus(AllocationStatus.CANCELLED);
            allocation.setClosedBy(cancelledBy != null ? cancelledBy : "SYSTEM");
            allocation.setClosedAt(LocalDateTime.now());
            
            ResourceAllocation cancelledAllocation = allocationRepository.save(allocation);
            updateAvailabilityLedgerForAllocation(cancelledAllocation);

            // If this was a PLANNED allocation cancelled before activation,
            // the demand status will be recalculated by checkAndUpdateDemandFulfillment
            // which will revert it to APPROVED if no longer fulfilled
            if (cancelledAllocation.getDemand() != null) {
                checkAndUpdateDemandFulfillment(cancelledAllocation.getDemand().getDemandId());
                log.info("PLANNED allocation {} cancelled. Demand {} fulfillment status recalculated.",
                        allocationId, cancelledAllocation.getDemand().getDemandId());
            }

            ApiResponse<Object> response = new ApiResponse<>();
            return ResponseEntity.ok(response.getAPIResponse(true, "Allocation cancelled successfully", mapToResponseDTO(cancelledAllocation)));
        } catch (Exception e) {
            log.error("Error cancelling allocation {}: {}", allocationId, e.getMessage());
            ApiResponse<Object> response = new ApiResponse<>();
            return ResponseEntity.internalServerError().body(response.getAPIResponse(false, "Error cancelling allocation", null));
        }
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "active-allocations", allEntries = true),
        @CacheEvict(value = "dashboard-kpis",     allEntries = true),
        @CacheEvict(value = "bench-resources",    allEntries = true),
        @CacheEvict(value = "bench-matches",      allEntries = true),
        @CacheEvict(value = "resource-timelines", allEntries = true),
        @CacheEvict(value = "demands",            allEntries = true)
    })
    public ResponseEntity<ApiResponse<?>> approveAllocation(UUID allocationId, String dmName) {

        ResourceAllocation allocation = allocationRepository.findById(allocationId)
                .orElseThrow(() -> new RuntimeException("Allocation not found"));

        if (allocation.getApprovalStatus() != ApprovalStatus.PENDING) {
            throw new RuntimeException("Not in pending state");
        }

        ResourceState state = resourceStateRepository
                .findByResourceIdAndCurrentFlagTrue(allocation.getResource().getResourceId())
                .orElseThrow(() -> new RuntimeException("Resource " + allocation.getResource().getResourceId() + " has no state record. Please initialize resource state first."));

        allocation.setApprovalStatus(ApprovalStatus.APPROVED);
        allocation.setApprovalActionBy(dmName);
        allocation.setApprovalActionAt(LocalDateTime.now());
        allocation.setAllocationStatus(AllocationStatus.ACTIVE);

        int currentInternal = state.getInternalAllocationPercentage() != null
                ? state.getInternalAllocationPercentage()
                : 0;

        int requested = allocation.getAllocationPercentage();

        int updatedInternal = Math.max(0, currentInternal - requested);
        state.setInternalAllocationPercentage(updatedInternal);

        if (updatedInternal == 0 && state.getStateType() == StateType.POOL) {
            moveToProjectState(state);
        }

        resourceStateRepository.save(state);
        allocationRepository.save(allocation);

        ApiResponse<Object> response = new ApiResponse<>();
        return ResponseEntity.ok(response.getAPIResponse(true, "Approved", null));
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "active-allocations", allEntries = true),
        @CacheEvict(value = "dashboard-kpis",     allEntries = true),
        @CacheEvict(value = "resource-timelines", allEntries = true)
    })
    public ResponseEntity<ApiResponse<?>> rejectAllocation(UUID allocationId, String reason, String dmName) {

        ResourceAllocation allocation = allocationRepository.findById(allocationId)
                .orElseThrow(() -> new RuntimeException("Allocation not found"));

        if (allocation.getApprovalStatus() != ApprovalStatus.PENDING) {
            throw new RuntimeException("Not in pending state");
        }

        if (reason == null || reason.isBlank()) {
            ApiResponse<Object> response = new ApiResponse<>();
            return ResponseEntity.badRequest()
                    .body(response.getAPIResponse(false, "Rejection reason required", null));
        }

        allocation.setApprovalStatus(ApprovalStatus.REJECTED);
        allocation.setRejectionReason(reason);
        allocation.setApprovalActionBy(dmName);
        allocation.setApprovalActionAt(LocalDateTime.now());

        allocationRepository.save(allocation);

        ApiResponse<Object> response = new ApiResponse<>();
        return ResponseEntity.ok(response.getAPIResponse(true, "Rejected", null));
    }

    private void moveToProjectState(ResourceState currentState) {

        currentState.setCurrentFlag(false);
        currentState.setEffectiveTo(LocalDate.now());

        ResourceState newState = ResourceState.builder()
                .resourceId(currentState.getResourceId())
                .stateType(StateType.PROJECT)
                .subState(SubState.READY)
                .effectiveFrom(LocalDate.now())
                .currentFlag(true)
                .createdBy("SYSTEM")
                .build();

        resourceStateRepository.save(currentState);
        resourceStateRepository.save(newState);
    }

    @Override
    public ResponseEntity<ApiResponse<?>> getPendingApprovals() {

        List<ResourceAllocation> list =
                allocationRepository.findByApprovalStatus(ApprovalStatus.PENDING);

        ApiResponse<Object> response = new ApiResponse<>();
        return ResponseEntity.ok(response.getAPIResponse(true, "Pending approvals", list));
    }

    @Override
    public ResponseEntity<ApiResponse<?>> getAllocationsByResource(String resourceId) {
        try {
            List<AllocationResponseDTO> responseList = allocationRepository.findByResource_ResourceId(resourceId).stream()
                    .map(this::mapToResponseDTO).collect(Collectors.toList());
            ApiResponse<Object> response = new ApiResponse<>();
            return ResponseEntity.ok(response.getAPIResponse(true, "Allocations retrieved successfully", responseList));
        } catch (Exception e) {
            log.error("Error retrieving allocations for resource {}: {}", resourceId, e.getMessage());
            ApiResponse<Object> response = new ApiResponse<>();
            return ResponseEntity.internalServerError().body(response.getAPIResponse(false, "Error retrieving allocations", null));
        }
    }

    @Override
    public ResponseEntity<ApiResponse<?>> getOverrideAllocations() {
        ApiResponse<Object> response = new ApiResponse<>();
        return ResponseEntity.ok(response.getAPIResponse(true, "Override allocations retrieved", allocationModificationRepository.findByOverrideFlagTrue()));
    }

    @Override
    public ResponseEntity<ApiResponse<?>> getAllocationsByDemand(UUID demandId) {
        try {
            List<AllocationResponseDTO> responseList = allocationRepository.findByDemand_DemandId(demandId).stream()
                    .map(this::mapToResponseDTO).collect(Collectors.toList());
            ApiResponse<Object> response = new ApiResponse<>();
            return ResponseEntity.ok(response.getAPIResponse(true, "Allocations retrieved successfully", responseList));
        } catch (Exception e) {
            log.error("Error retrieving allocations for demand {}: {}", demandId, e.getMessage());
            ApiResponse<Object> response = new ApiResponse<>();
            return ResponseEntity.internalServerError().body(response.getAPIResponse(false, "Error retrieving allocations", null));
        }
    }

    @Override
    public ResponseEntity<ApiResponse<?>> getAllocationsByProject(Long projectId) {
        try {
            List<AllocationResponseDTO> responseList = allocationRepository.findByProject_PmsProjectId(projectId).stream()
                    .map(this::mapToResponseDTO).collect(Collectors.toList());
            ApiResponse<Object> response = new ApiResponse<>();
            return ResponseEntity.ok(response.getAPIResponse(true, "Allocations retrieved successfully", responseList));
        } catch (Exception e) {
            log.error("Error retrieving allocations for project {}: {}", projectId, e.getMessage());
            ApiResponse<Object> response = new ApiResponse<>();
            return ResponseEntity.internalServerError().body(response.getAPIResponse(false, "Error retrieving allocations", null));
        }
    }

    @Override
    public ResponseEntity<ApiResponse<?>> analyzeSkillGap(SkillGapAnalysisRequestDTO request) {
        try {
            Demand demand = demandRepository.findById(request.getDemandId())
                .orElseThrow(() -> new AllocationExceptionHandler(HttpStatus.NOT_FOUND, "DEMAND_NOT_FOUND", "Demand not found"));

            if (!resourceRepository.existsById(request.getResourceId())) {
                throw new AllocationExceptionHandler(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Resource not found");
            }

            ApiResponse<?> response = new ApiResponse<>(true, "Skill gap analysis completed", skillGapService.performSkillGapAnalysis(demand, request.getResourceId()));
            return ResponseEntity.ok(response);
        } catch (AllocationExceptionHandler e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to analyze skill gap: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("Failed to analyze skill gap"));
        }
    }

    @Override
    public ConflictDetectionResult detectPriorityConflicts(AllocationRequestDTO allocationRequest) {
        return conflictService.detectPriorityConflicts(allocationRequest, allocationRequest.getResourceId().get(0));
    }

    @Override
    public List<AllocationConflictDTO> detectAllocationConflicts(String resourceId) {
        return conflictService.detectAllocationConflicts(resourceId);
    }

    @Override
    public ResponseEntity<ApiResponse<?>> resolveAllocationConflict(UUID conflictId, ConflictResolutionDTO resolution) {
        return conflictService.resolveAllocationConflict(conflictId, resolution);
    }

    @Override
    public List<AllocationConflictDTO> getPendingConflictsForResource(String resourceId) {
        return conflictService.getPendingConflictsForResource(resourceId);
    }

    @Override
    public List<AllocationConflictDTO> getAllPendingConflicts() {
        return conflictService.getAllPendingConflicts();
    }

    @Override
    public ResponseEntity<ApiResponse<?>> quickAllocateResource(String resourceId, UUID demandId, Integer allocationPercentage, UserDTO user) {
        log.info("Quick allocating resource {} to demand {} by user {} with {}% allocation",
                resourceId, demandId, user.getName(), allocationPercentage);

        // Fetch demand to get allocation percentage if not provided
        Integer finalAllocationPercentage = allocationPercentage;
        if (finalAllocationPercentage == null && demandId != null) {
            Optional<Demand> demandOpt = demandRepository.findById(demandId);
            if (demandOpt.isPresent()) {
                finalAllocationPercentage = demandOpt.get().getAllocationPercentage();
                log.info("Auto-populated allocation percentage from demand for quick allocation: {}%", finalAllocationPercentage);
            }
        }

        // Create quick allocation DTO from parameters
        QuickAllocationDTO quickAllocation = QuickAllocationDTO.builder()
                .resourceId(resourceId)
                .demandId(demandId)
                .allocationPercentage(finalAllocationPercentage)
                .build();
        
        // Convert quick allocation to full allocation request
        AllocationRequestDTO allocationRequest = buildAllocationRequest(quickAllocation, user);

        // Use existing allocation service with full validation
        return assignAllocation(allocationRequest);
    }

    /**
     * Build full AllocationRequestDTO from QuickAllocationDTO with smart defaults
     */
    private AllocationRequestDTO buildAllocationRequest(QuickAllocationDTO quickAllocation, UserDTO user) {
        return AllocationRequestDTO.builder()
                .resourceId(List.of(quickAllocation.getResourceId()))
                .demandId(quickAllocation.getDemandId())
                .allocationPercentage(quickAllocation.getAllocationPercentage())
                .allocationStartDate(calculateStartDate(quickAllocation.getDemandId()))
                .allocationEndDate(calculateEndDate(quickAllocation.getDemandId()))
                .allocationStatus(AllocationStatus.ACTIVE)
                .allocationType(AllocationType.ACTIVE)
                .createdBy(user.getName())
                .skipValidation(false)
                .build();
    }

    /**
     * Calculate smart start date based on demand
     */
    private java.time.LocalDate calculateStartDate(UUID demandId) {
        return demandRepository.findById(demandId)
                .map(demand -> {
                    java.time.LocalDate demandStart = demand.getDemandStartDate();
                    java.time.LocalDate today = java.time.LocalDate.now();
                    // Use demand start date if it's today or future, otherwise start today
                    return demandStart.isAfter(today) ? demandStart : today;
                })
                .orElseGet(java.time.LocalDate::now); // Fallback to today if demand not found
    }

    /**
     * Calculate smart end date based on demand
     */
    private java.time.LocalDate calculateEndDate(UUID demandId) {
        return demandRepository.findById(demandId)
                .map(demand -> demand.getDemandEndDate())
                .orElseGet(() -> java.time.LocalDate.now().plusMonths(6)); // Fallback to 6 months
    }


    @Override
    public ResponseEntity<ApiResponse<?>> getProjectResources(Long projectId) {
        List<ResourceNameDTO> resources = allocationRepository.findResourcesByProjectId(projectId).stream()
                .map(r -> new ResourceNameDTO(r.getFullName(), r.getResourceId(), r.getDesignation())).collect(Collectors.toList());
        return ResponseEntity.ok(new ApiResponse<>(true, "Resources by Project Id", resources));
    }

    private List<ResourceAllocation> persistAllocations(List<ResourceAllocation> validAllocations) {

        List<ResourceAllocation> finalList = new ArrayList<>();

        for (ResourceAllocation allocation : validAllocations) {

            String resourceId = allocation.getResource().getResourceId();

            ResourceState state = resourceStateRepository
                    .findByResourceIdAndCurrentFlagTrue(resourceId)
                    .orElseThrow(() -> new RuntimeException("Resource " + resourceId + " has no state record. Please initialize resource state first."));

            int internal = state.getInternalAllocationPercentage() != null
                    ? state.getInternalAllocationPercentage()
                    : 0;

            Integer currentProject = allocationRepository
                    .getActiveAllocationPercentage(resourceId, allocation.getAllocationStartDate());

            if (currentProject == null) currentProject = 0;

            int requested = allocation.getAllocationPercentage();
            int totalAllocation = internal + currentProject + requested;

            // Set allocation type and planned start date from the request
            // These should already be set by the validation service, but ensure they're set
            if (allocation.getAllocationType() == null) {
                allocation.setAllocationType(AllocationType.ACTIVE);
            }

            // ✅ WITHIN NORMAL CAPACITY (<=100%)
            if (totalAllocation <= 100) {
                allocation.setApprovalStatus(ApprovalStatus.NOT_REQUIRED);
                // For ACTIVE type, set to ACTIVE; for PLANNED type, set to PLANNED
                if (allocation.getAllocationType() == AllocationType.PLANNED) {
                    allocation.setAllocationStatus(AllocationStatus.PLANNED);
                } else {
                    allocation.setAllocationStatus(AllocationStatus.ACTIVE);
                }
            }
            // ⚠ EXCEEDS 100% BUT <=130% (SPECIAL CONDITIONS)
            else if (totalAllocation <= 130) {
                // Check if beyond capacity approval is granted
                if (Boolean.TRUE.equals(allocation.getRequestBeyondCapacityApproval())) {
                    allocation.setApprovalStatus(ApprovalStatus.APPROVED);
                    // For ACTIVE type, set to ACTIVE; for PLANNED type, set to PLANNED
                    if (allocation.getAllocationType() == AllocationType.PLANNED) {
                        allocation.setAllocationStatus(AllocationStatus.PLANNED);
                    } else {
                        allocation.setAllocationStatus(AllocationStatus.ACTIVE);
                    }
                } else {
                    allocation.setApprovalStatus(ApprovalStatus.PENDING);
                    allocation.setAllocationStatus(AllocationStatus.PLANNED);
                }
            }
            // ❌ EXCEEDS 130% - SHOULD NOT REACH HERE DUE TO VALIDATION
            else {
                throw new RuntimeException("Allocation exceeds 130% limit - this should have been caught in validation");
            }

            finalList.add(allocation);
        }

        List<ResourceAllocation> saved = allocationRepository.saveAll(finalList);

        for (int i = 0; i < saved.size(); i++) {
            finalList.get(i).setAllocationId(saved.get(i).getAllocationId());
        }

        return finalList;
    }

    private void triggerAsyncLedgerUpdate(List<ResourceAllocation> savedAllocations) {
        for (ResourceAllocation allocation : savedAllocations) {
            ledgerAsyncService.updateLedgerAsync(allocation);
        }
    }

    private ResponseEntity<ApiResponse<?>> buildAllocationResponse(List<ResourceAllocation> savedAllocations, List<AllocationFailure> failures) {
        int successCount = savedAllocations.size();
        int failureCount = failures.size();
        String message = (failureCount == 0) ? "Allocation Successful" : (successCount == 0) ? "Allocation Failed" : "Allocation Partially Successful";

        List<UUID> ids = savedAllocations.stream().map(ResourceAllocation::getAllocationId).collect(Collectors.toList());
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("successCount", successCount);
        responseData.put("failureCount", failureCount);
        responseData.put("savedAllocations", allocationRepository.findByAllocationIdIn(ids).stream().map(this::mapToResponseDTO).collect(Collectors.toList()));
        responseData.put("failedResources", failures);

        ApiResponse<?> response = new ApiResponse<>(true, message, responseData);
        return ResponseEntity.ok(response);
    }

    private void validateResourceCapacityForUpdate(UUID allocationId, AllocationRequestDTO request) {
        // Get existing conflicting allocations excluding the current one
        int existingProjectAllocations = allocationRepository.findConflictingAllocationsForResources(request.getResourceId(), request.getAllocationStartDate(), request.getAllocationEndDate())
                .stream().filter(a -> !a.getAllocationId().equals(allocationId)).mapToInt(ResourceAllocation::getAllocationPercentage).sum();
        
        // Get internal pool allocation
        ResourceState state = resourceStateRepository
                .findByResourceIdAndCurrentFlagTrue(request.getResourceId().get(0))
                .orElseThrow(() -> new AllocationExceptionHandler(HttpStatus.BAD_REQUEST, "RESOURCE_STATE_MISSING", "Resource state not found"));
        
        int internalAllocation = state.getInternalAllocationPercentage() != null ? state.getInternalAllocationPercentage() : 0;
        int requested = request.getAllocationPercentage();
        int totalAllocation = internalAllocation + existingProjectAllocations + requested;
        
        if (totalAllocation > 130) {
            throw new AllocationExceptionHandler(HttpStatus.BAD_REQUEST, "MAX_ALLOCATION_EXCEEDED", 
                String.format("Total allocation would be %d%% (Internal: %d%%, Project: %d%%, Requested: %d%%) which exceeds the maximum allowed limit of 130%%",
                    totalAllocation, internalAllocation, existingProjectAllocations, requested));
        }
        
        if (totalAllocation > 100 && !Boolean.TRUE.equals(request.getRequestBeyondCapacityApproval())) {
            throw new AllocationExceptionHandler(HttpStatus.BAD_REQUEST, "CAPACITY_APPROVAL_REQUIRED", 
                "Allocation exceeds 100% and requires special condition approval");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
        @CacheEvict(value = "active-allocations", allEntries = true),
        @CacheEvict(value = "dashboard-kpis", allEntries = true),
        @CacheEvict(value = "bench-resources", allEntries = true),
        @CacheEvict(value = "bench-matches", allEntries = true),
        @CacheEvict(value = "resource-timelines", allEntries = true),
        @CacheEvict(value = "demands", allEntries = true)
    })
    public ResponseEntity<ApiResponse<?>> closeAllocation(UUID allocationId, CloseAllocationDTO request) {
        Optional<ResourceAllocation> allocationOpt = allocationRepository.findById(allocationId);
        if (allocationOpt.isEmpty()) return ResponseEntity.notFound().build();

        ResourceAllocation allocation = allocationOpt.get();

        if (allocation.getAllocationStatus() == AllocationStatus.ENDED ||
                allocation.getAllocationStatus() == AllocationStatus.DELETED ||
                allocation.getAllocationStatus() == AllocationStatus.CANCELLED ||
                allocation.getAllocationStatus() == AllocationStatus.ROLLED_OFF) {

            return ResponseEntity.badRequest().body(
                    new ApiResponse<>(false,
                            "Allocation is already closed or deleted", null));
        }

        LocalDate closureDate = request.getClosureDate() != null ? request.getClosureDate() : LocalDate.now();
        if (closureDate.isBefore(allocation.getAllocationStartDate())) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Closure date cannot be before start date", null));
        }

        allocation.setAllocationEndDate(closureDate);
        // Use ROLLED_OFF status for role-off closures, ENDED for normal closures
        if ("ROLE_OFF".equals(request.getReason())) {
            allocation.setAllocationStatus(AllocationStatus.ROLLED_OFF);
        } else {
            allocation.setAllocationStatus(AllocationStatus.ENDED);
        }
        allocation.setClosedBy("SYSTEM");
        allocation.setClosedAt(LocalDateTime.now());
        allocation.setClosureReason(request.getReason() != null ? request.getReason() : "Manual closure");

        ResourceAllocation saved = allocationRepository.save(allocation);
        ledgerAsyncService.synchronizeAvailabilityAcrossModules(saved.getResource().getResourceId(), closureDate);

        ApiResponse<?> response = new ApiResponse<>(true, "Allocation closed successfully", mapToResponseDTO(saved));
        return ResponseEntity.ok(response);
    }

    @Override
    public void updateAvailabilityLedgerForAllocation(ResourceAllocation allocation) {
        ledgerAsyncService.updateLedgerAsync(allocation);
    }

    @Override
    public void checkAndUpdateDemandFulfillment(UUID demandId) {
        Demand demand = demandRepository.findById(demandId).orElse(null);
        if (demand == null) return;

        // Count both ACTIVE and PLANNED allocations for demand fulfillment
        // This allows demand to be FULFILLED when all resources are allocated,
        // regardless of whether they are ACTIVE or PLANNED
        List<ResourceAllocation> allocated = allocationRepository.findByDemand_DemandId(demandId).stream()
                .filter(a -> a.getAllocationStatus() == AllocationStatus.ACTIVE || a.getAllocationStatus() == AllocationStatus.PLANNED)
                .toList();

        // Check if demand is fulfilled: enough allocations with correct percentage
        boolean isFulfilled = allocated.size() >= demand.getResourcesRequired()
                && allocated.stream().allMatch(a -> a.getAllocationPercentage().equals(demand.getAllocationPercentage()));

        if (isFulfilled) {
            if (demand.getDemandStatus() != DemandStatus.FULFILLED) {
                demand.setDemandStatus(DemandStatus.FULFILLED);
                demandRepository.save(demand);
                demandSLARepository.findByDemand_DemandIdAndActiveFlagTrue(demandId).ifPresent(sla -> {
                    sla.setActiveFlag(false);
                    sla.setFulfillDate(LocalDate.now());
                    demandSLARepository.save(sla);
                });
            }
        } else if (demand.getDemandStatus() == DemandStatus.FULFILLED) {
            // If not fulfilled but was marked as FULFILLED, revert to APPROVED
            // This handles the case when a PLANNED allocation is cancelled
            demand.setDemandStatus(DemandStatus.APPROVED);
            demandRepository.save(demand);
        }
    }

    private AllocationResponseDTO mapToResponseDTO(ResourceAllocation allocation) {
        AllocationResponseDTO dto = new AllocationResponseDTO();
        dto.setAllocationId(allocation.getAllocationId());
        if (allocation.getResource() != null) {
            dto.setFullName(allocation.getResource().getFullName());
            dto.setEmail(allocation.getResource().getEmail());
            dto.setRemainingAllocationPercentage(calculateRemainingAllocationPercentage(allocation.getResource().getResourceId(), allocation.getAllocationStartDate(), allocation.getAllocationEndDate(), allocation.getAllocationId()));
        }
        if (allocation.getDemand() != null) dto.setDemandName(allocation.getDemand().getDemandName());
        dto.setAllocationStartDate(allocation.getAllocationStartDate());
        dto.setAllocationEndDate(allocation.getAllocationEndDate());
        dto.setAllocationPercentage(allocation.getAllocationPercentage());
        dto.setAllocationStatus(allocation.getAllocationStatus().name());
        dto.setAllocationType(allocation.getAllocationType() != null ? allocation.getAllocationType().name() : null);
        dto.setPlannedStartDate(allocation.getPlannedStartDate());
        dto.setCreatedBy(allocation.getCreatedBy());
        return dto;
    }

    private Integer calculateRemainingAllocationPercentage(String resourceId, LocalDate startDate, LocalDate endDate, UUID currentId) {
        try {
            int total = allocationRepository.findConflictingAllocations(resourceId, startDate, endDate)
                    .stream().filter(a -> a.getAllocationId().equals(currentId)).mapToInt(ResourceAllocation::getAllocationPercentage).sum();
            return Math.max(0, 100 - total);
        } catch (Exception e) {
            log.error("Error calculating remaining capacity for {}: {}", resourceId, e.getMessage());
            return 130;
        }
    }

    /**
     * Validate and set allocation type logic
     * - ACTIVE: allocationStartDate must be today or in the past, set to today if in past
     * - PLANNED: allocationStartDate must be in the future, use plannedStartDate
     *
     * AUTO-INFERENCE: If allocationStatus is PLANNED and allocationType is not set,
     * automatically set allocationType to PLANNED for better UX
     */
    private void validateAndSetAllocationType(AllocationRequestDTO allocationRequest) {
        LocalDate today = LocalDate.now();

        // Auto-infer allocationType from allocationStatus for better UX
        if (allocationRequest.getAllocationType() == null) {
            if (allocationRequest.getAllocationStatus() == AllocationStatus.PLANNED) {
                // User sent PLANNED status without allocationType, auto-set to PLANNED
                allocationRequest.setAllocationType(AllocationType.PLANNED);
                log.info("Auto-inferred allocationType=PLANNED from allocationStatus=PLANNED");
            } else {
                // Default to ACTIVE for other statuses
                allocationRequest.setAllocationType(AllocationType.ACTIVE);
            }
        }

        if (allocationRequest.getAllocationType() == AllocationType.ACTIVE) {
            // ACTIVE allocation: start date must be today or in the past
            if (allocationRequest.getAllocationStartDate() == null) {
                allocationRequest.setAllocationStartDate(today);
            } else if (allocationRequest.getAllocationStartDate().isAfter(today)) {
                throw new AllocationExceptionHandler(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_ACTIVE_ALLOCATION_DATE",
                    "ACTIVE allocation cannot have a future start date. Use PLANNED allocation type for future dates."
                );
            } else if (allocationRequest.getAllocationStartDate().isBefore(today)) {
                // If start date is in the past, set it to today
                allocationRequest.setAllocationStartDate(today);
            }
            // plannedStartDate should be null for ACTIVE type
            allocationRequest.setPlannedStartDate(null);
        } else if (allocationRequest.getAllocationType() == AllocationType.PLANNED) {
            // PLANNED allocation: must have plannedStartDate in the future
            if (allocationRequest.getPlannedStartDate() == null) {
                // Auto-infer plannedStartDate from allocationStartDate if provided and is in future
                if (allocationRequest.getAllocationStartDate() != null && allocationRequest.getAllocationStartDate().isAfter(today)) {
                    allocationRequest.setPlannedStartDate(allocationRequest.getAllocationStartDate());
                    log.info("Auto-populated plannedStartDate={} from allocationStartDate for PLANNED allocation", allocationRequest.getAllocationStartDate());
                } else {
                    throw new AllocationExceptionHandler(
                        HttpStatus.BAD_REQUEST,
                        "PLANNED_START_DATE_REQUIRED",
                        "PLANNED allocation requires a planned start date in the future. Please provide either 'plannedStartDate' or 'allocationStartDate' with a future date."
                    );
                }
            }
            if (allocationRequest.getPlannedStartDate().isBefore(today) || allocationRequest.getPlannedStartDate().isEqual(today)) {
                throw new AllocationExceptionHandler(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_PLANNED_START_DATE",
                    "PLANNED allocation must have a start date in the future."
                );
            }
            // Set allocationStartDate to today for PLANNED type (allocation doesn't actually start until plannedStartDate)
            allocationRequest.setAllocationStartDate(today);
        }
    }

    /**
     * Cached service method wrapper for repository query
     */
    @Cacheable(value = "active-allocations", key = "#resourceId + '-' + #date")
    public List<ResourceAllocation> findActiveAllocationsForResourceOnDate(String resourceId, LocalDate date) {
        return allocationRepository.findActiveAllocationsForResourceOnDate(resourceId, date);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "active-allocations", allEntries = true),
        @CacheEvict(value = "dashboard-kpis",     allEntries = true),
        @CacheEvict(value = "bench-resources",    allEntries = true),
        @CacheEvict(value = "bench-matches",      allEntries = true),
        @CacheEvict(value = "resource-timelines", allEntries = true),
        @CacheEvict(value = "demands",            allEntries = true)
    })
    public void processAutoClosures() {
        try {
            LocalDate today = LocalDate.now();
            int updated = allocationRepository.autoCloseAllocations(today, AllocationStatus.ENDED);
            if (updated > 0) {
                log.info("Auto-closed {} expired allocations", updated);
            }
        } catch (Exception e) {
            log.error("Failed to process auto-closures: {}", e.getMessage());
        }
    }

    /**
     * Automatically activate PLANNED allocations whose planned start date has arrived
     * This should run daily to check for PLANNED allocations that need to be activated
     */
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "active-allocations", allEntries = true),
        @CacheEvict(value = "dashboard-kpis",     allEntries = true),
        @CacheEvict(value = "bench-resources",    allEntries = true),
        @CacheEvict(value = "bench-matches",      allEntries = true),
        @CacheEvict(value = "resource-timelines", allEntries = true),
        @CacheEvict(value = "demands",            allEntries = true)
    })
    public void activatePlannedAllocations() {
        try {
            LocalDate today = LocalDate.now();

            // Use optimized repository query to find PLANNED allocations that should be activated today
            List<ResourceAllocation> plannedAllocations = allocationRepository.findPlannedAllocationsToActivate(today);

            if (plannedAllocations.isEmpty()) {
                log.debug("No PLANNED allocations to activate today");
                return;
            }

            log.info("Found {} PLANNED allocations to activate", plannedAllocations.size());
            int activatedCount = 0;

            for (ResourceAllocation allocation : plannedAllocations) {
                try {
                    // Update allocation status to ACTIVE
                    allocation.setAllocationStatus(AllocationStatus.ACTIVE);
                    allocationRepository.save(allocation);

                    // Move resource to project state
                    if (allocation.getResource() != null) {
                        benchDetectionService.moveToProject(allocation.getResource().getResourceId(), allocation.getAllocationId());
                    }

                    // Update availability ledger
                    updateAvailabilityLedgerForAllocation(allocation);

                    activatedCount++;
                    log.info("Activated PLANNED allocation {} for resource {}",
                            allocation.getAllocationId(), allocation.getResource().getResourceId());
                } catch (Exception e) {
                    log.error("Failed to activate PLANNED allocation {}: {}", allocation.getAllocationId(), e.getMessage());
                }
            }

            log.info("Successfully activated {} PLANNED allocations", activatedCount);
        } catch (Exception e) {
            log.error("Failed to process PLANNED allocation activation: {}", e.getMessage());
        }
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "active-allocations", allEntries = true),
            @CacheEvict(value = "dashboard-kpis",     allEntries = true),
            @CacheEvict(value = "bench-resources",    allEntries = true),
            @CacheEvict(value = "bench-matches",      allEntries = true),
            @CacheEvict(value = "resource-timelines", allEntries = true),
            @CacheEvict(value = "demands",            allEntries = true)
    })
    public ResponseEntity<ApiResponse<?>> deleteAllocation(UUID allocationId, UserDTO user) {

        try {

            Optional<ResourceAllocation> allocationOpt =
                    allocationRepository.findById(allocationId);

            if (allocationOpt.isEmpty()) {
                ApiResponse<Object> response = new ApiResponse<>();
                return ResponseEntity.notFound().build();
            }

            ResourceAllocation allocation = allocationOpt.get();

            // Prevent deletion of ACTIVE allocations
            if (allocation.getAllocationStatus() == AllocationStatus.ACTIVE) {

                ApiResponse<Object> response = new ApiResponse<>();

                return ResponseEntity.badRequest().body(
                        response.getAPIResponse(
                                false,
                                "Cannot delete ACTIVE allocation. Please use cancel or close instead.",
                                null
                        )
                );
            }

            // Prevent duplicate deletion
            if (allocation.getAllocationStatus() == AllocationStatus.DELETED) {

                ApiResponse<Object> response = new ApiResponse<>();

                return ResponseEntity.badRequest().body(
                        response.getAPIResponse(
                                false,
                                "Allocation is already deleted",
                                null
                        )
                );
            }

            UUID demandId = allocation.getDemand() != null
                    ? allocation.getDemand().getDemandId()
                    : null;

            String resourceId = allocation.getResource() != null
                    ? allocation.getResource().getResourceId()
                    : null;

            // Create audit record
            AllocationModification deletionRecord = new AllocationModification();

            deletionRecord.setAllocation(allocation);
            deletionRecord.setCurrentAllocationPercentage(
                    allocation.getAllocationPercentage());

            deletionRecord.setReason(
                    "Resource Allocation Deleted from "
                            + allocation.getAllocationStatus()
                            + " status");

            deletionRecord.setRequestedBy(user.getName());
            deletionRecord.setRequestedAt(LocalDateTime.now());
            deletionRecord.setStatus(AllocationModificationStatus.DELETED);

            allocationModificationRepository.save(deletionRecord);

            // SOFT DELETE
            allocation.setAllocationStatus(AllocationStatus.DELETED);
            allocation.setClosedBy(user.getName());
            allocation.setClosedAt(LocalDateTime.now());

            ResourceAllocation deletedAllocation =
                    allocationRepository.save(allocation);

            // Update demand fulfillment
            if (demandId != null) {

                checkAndUpdateDemandFulfillment(demandId);

                log.info(
                        "Allocation {} deleted. Demand {} fulfillment recalculated.",
                        allocationId,
                        demandId
                );
            }

            // Trigger bench detection
            if (resourceId != null) {
                benchDetectionService.detectBenchResources();
            }

            ApiResponse<Object> response = new ApiResponse<>();

            return ResponseEntity.ok(
                    response.getAPIResponse(
                            true,
                            "Allocation deleted successfully",
                            mapToResponseDTO(deletedAllocation)
                    )
            );

        } catch (Exception e) {

            log.error("Error deleting allocation {}", allocationId, e);

            ApiResponse<Object> response = new ApiResponse<>();

            return ResponseEntity.internalServerError().body(
                    response.getAPIResponse(
                            false,
                            "Error deleting allocation",
                            null
                    )
            );
        }
    }
}
