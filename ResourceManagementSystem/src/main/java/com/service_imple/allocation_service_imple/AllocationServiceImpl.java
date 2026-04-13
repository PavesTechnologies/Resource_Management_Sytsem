package com.service_imple.allocation_service_imple;

import com.dto.allocation_dto.*;
import com.dto.centralised_dto.ApiResponse;
import com.dto.resource.ResourceNameDTO;
import com.entity.allocation_entities.AllocationModification;
import com.entity.allocation_entities.ResourceAllocation;
import com.entity.bench.ResourceState;
import com.entity.demand_entities.Demand;
import com.entity.demand_entities.DemandSLA;
import com.entity_enums.allocation_enums.AllocationStatus;
import com.entity_enums.allocation_enums.ApprovalStatus;
import com.entity_enums.bench.StateType;
import com.entity_enums.bench.SubState;
import com.entity_enums.demand_enums.DemandStatus;
import com.global_exception_handler.ProjectExceptionHandler;
import com.repo.allocation_repo.AllocationRepository;
import com.repo.bench_repo.ResourceStateRepository;
import com.repo.demand_repo.DemandSLARepository;
import com.repo.allocation_repo.AllocationModificationRepository;
import com.repo.resource_repo.ResourceRepository;
import com.repo.demand_repo.DemandRepository;
import com.service_interface.allocation_service_interface.AllocationService;
import com.service_interface.ledger_service_interface.AvailabilityCalculationService;
import com.service_imple.skill_service_impl.ResourceSkillUsageService;
import com.service_imple.bench_service_impl.BenchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final AvailabilityLedgerAsyncServiceRefactored ledgerAsyncService;
    private final DemandSLARepository demandSLARepository;
    private final AvailabilityCalculationService availabilityCalculationService;
    private final BenchService benchDetectionService;
    private final ResourceSkillUsageService resourceSkillUsageService;
    private final ResourceStateRepository resourceStateRepository;

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<?>> assignAllocation(AllocationRequestDTO allocationRequest) {
        try {
            validationService.validateRequest(allocationRequest);
            DemandProjectData demandProjectData = validationService.validateDemandOrProject(allocationRequest);
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

        } catch (ProjectExceptionHandler e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error creating allocation: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(new ApiResponse<>(false, "Error creating allocation", null));
        }
    }

    @Override
    public ResponseEntity<ApiResponse<?>> getAllocationById(UUID allocationId) {
        try {
            Optional<ResourceAllocation> allocation = allocationRepository.findById(allocationId);
            if (allocation.isPresent()) {
                ApiResponse<?> response = new ApiResponse<>(true, "Allocation found", mapToResponseDTO(allocation.get()));
                return ResponseEntity.ok(response);
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error retrieving allocation {}: {}", allocationId, e.getMessage());
            return ResponseEntity.internalServerError().body(new ApiResponse<>(false, "Error retrieving allocation", null));
        }
    }

    @Override
    public ResponseEntity<ApiResponse<?>> updateAllocation(UUID allocationId, AllocationRequestDTO allocationRequest) {
        try {
            Optional<ResourceAllocation> existingAllocation = allocationRepository.findById(allocationId);
            if (existingAllocation.isEmpty()) return ResponseEntity.notFound().build();

            ResourceAllocation allocation = existingAllocation.get();
            if (allocation.getAllocationStatus() == AllocationStatus.ENDED || allocation.getAllocationStatus() == AllocationStatus.CANCELLED) {
                return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Closed or cancelled allocations cannot be modified", null));
            }

            allocation.setAllocationStartDate(allocationRequest.getAllocationStartDate());
            allocation.setAllocationEndDate(allocationRequest.getAllocationEndDate());
            allocation.setAllocationPercentage(allocationRequest.getAllocationPercentage());
            allocation.setAllocationStatus(allocationRequest.getAllocationStatus());

            if (allocationRequest.getAllocationStatus() == AllocationStatus.ENDED) {
                if (allocationRequest.getRoleOffReason() == null || allocationRequest.getRoleOffDate() == null) {
                    throw new ProjectExceptionHandler(HttpStatus.BAD_REQUEST, "ROLE_OFF_INFO_REQUIRED", "Role-off reason and date are required.");
                }
                allocation.setRoleOffReason(allocationRequest.getRoleOffReason());
                allocation.setRoleOffDate(allocationRequest.getRoleOffDate());
                allocation.setClosedBy("SYSTEM");
                allocation.setClosedAt(LocalDateTime.now());
            }

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

            ApiResponse<?> response = new ApiResponse<>(true, "Allocation updated successfully", mapToResponseDTO(updatedAllocation));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating allocation {}: {}", allocationId, e.getMessage());
            return ResponseEntity.internalServerError().body(new ApiResponse<>(false, "Error updating allocation", null));
        }
    }

    @Override
    public ResponseEntity<ApiResponse<?>> cancelAllocation(UUID allocationId, String cancelledBy) {
        try {
            Optional<ResourceAllocation> existingAllocation = allocationRepository.findById(allocationId);
            if (existingAllocation.isEmpty()) return ResponseEntity.notFound().build();

            ResourceAllocation allocation = existingAllocation.get();
            allocation.setAllocationStatus(AllocationStatus.CANCELLED);
            allocation.setClosedBy(cancelledBy != null ? cancelledBy : "SYSTEM");
            allocation.setClosedAt(LocalDateTime.now());
            
            ResourceAllocation cancelledAllocation = allocationRepository.save(allocation);
            updateAvailabilityLedgerForAllocation(cancelledAllocation);

            if (cancelledAllocation.getDemand() != null) {
                checkAndUpdateDemandFulfillment(cancelledAllocation.getDemand().getDemandId());
            }

            ApiResponse<?> response = new ApiResponse<>(true, "Allocation cancelled successfully", mapToResponseDTO(cancelledAllocation));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error cancelling allocation {}: {}", allocationId, e.getMessage());
            return ResponseEntity.internalServerError().body(new ApiResponse<>(false, "Error cancelling allocation", null));
        }
    }

    @Override
    @Transactional
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

        return ResponseEntity.ok(new ApiResponse<>(true, "Approved", null));
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<?>> rejectAllocation(UUID allocationId, String reason, String dmName) {

        ResourceAllocation allocation = allocationRepository.findById(allocationId)
                .orElseThrow(() -> new RuntimeException("Allocation not found"));

        if (allocation.getApprovalStatus() != ApprovalStatus.PENDING) {
            throw new RuntimeException("Not in pending state");
        }

        if (reason == null || reason.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Rejection reason required", null));
        }

        allocation.setApprovalStatus(ApprovalStatus.REJECTED);
        allocation.setRejectionReason(reason);
        allocation.setApprovalActionBy(dmName);
        allocation.setApprovalActionAt(LocalDateTime.now());

        allocationRepository.save(allocation);

        return ResponseEntity.ok(new ApiResponse<>(true, "Rejected", null));
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

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Pending approvals", list)
        );
    }

    @Override
    public ResponseEntity<ApiResponse<?>> getAllocationsByResource(Long resourceId) {
        try {
            List<AllocationResponseDTO> responseList = allocationRepository.findByResource_ResourceId(resourceId).stream()
                    .map(this::mapToResponseDTO).collect(Collectors.toList());
            ApiResponse<?> response = new ApiResponse<>(true, "Allocations retrieved successfully", responseList);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retrieving allocations for resource {}: {}", resourceId, e.getMessage());
            return ResponseEntity.internalServerError().body(new ApiResponse<>(false, "Error retrieving allocations", null));
        }
    }

    @Override
    public ResponseEntity<ApiResponse<?>> getOverrideAllocations() {
        return ResponseEntity.ok(ApiResponse.success("Override allocations retrieved", allocationModificationRepository.findByOverrideFlagTrue()));
    }

    @Override
    public ResponseEntity<ApiResponse<?>> getAllocationsByDemand(UUID demandId) {
        try {
            List<AllocationResponseDTO> responseList = allocationRepository.findByDemand_DemandId(demandId).stream()
                    .map(this::mapToResponseDTO).collect(Collectors.toList());
            ApiResponse<?> response = new ApiResponse<>(true, "Allocations retrieved successfully", responseList);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retrieving allocations for demand {}: {}", demandId, e.getMessage());
            return ResponseEntity.internalServerError().body(new ApiResponse<>(false, "Error retrieving allocations", null));
        }
    }

    @Override
    public ResponseEntity<ApiResponse<?>> getAllocationsByProject(Long projectId) {
        try {
            List<AllocationResponseDTO> responseList = allocationRepository.findByProject_PmsProjectId(projectId).stream()
                    .map(this::mapToResponseDTO).collect(Collectors.toList());
            ApiResponse<?> response = new ApiResponse<>(true, "Allocations retrieved successfully", responseList);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retrieving allocations for project {}: {}", projectId, e.getMessage());
            return ResponseEntity.internalServerError().body(new ApiResponse<>(false, "Error retrieving allocations", null));
        }
    }

    @Override
    public ResponseEntity<ApiResponse<?>> analyzeSkillGap(SkillGapAnalysisRequestDTO request) {
        try {
            Demand demand = demandRepository.findById(request.getDemandId())
                .orElseThrow(() -> new ProjectExceptionHandler(HttpStatus.NOT_FOUND, "DEMAND_NOT_FOUND", "Demand not found"));

            if (!resourceRepository.existsById(request.getResourceId())) {
                throw new ProjectExceptionHandler(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Resource not found");
            }

            ApiResponse<?> response = new ApiResponse<>(true, "Skill gap analysis completed", skillGapService.performSkillGapAnalysis(demand, request.getResourceId()));
            return ResponseEntity.ok(response);
        } catch (ProjectExceptionHandler e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Failed to analyze skill gap: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponse<>(false, "Failed to analyze skill gap", null));
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
        List<ResourceNameDTO> resources = allocationRepository.findResourcesByProjectId(projectId).stream()
                .map(r -> new ResourceNameDTO(r.getFullName(), r.getResourceId(), r.getDesignation())).collect(Collectors.toList());
        return ResponseEntity.ok(new ApiResponse<>(true, "Resources by Project Id", resources));
    }

    private List<ResourceAllocation> persistAllocations(List<ResourceAllocation> validAllocations) {

        List<ResourceAllocation> finalList = new ArrayList<>();

        for (ResourceAllocation allocation : validAllocations) {

            Long resourceId = allocation.getResource().getResourceId();

            ResourceState state = resourceStateRepository
                    .findByResourceIdAndCurrentFlagTrue(resourceId)
                    .orElseThrow(() -> new RuntimeException("Resource " + resourceId + " has no state record. Please initialize resource state first."));

            int internal = state.getInternalAllocationPercentage() != null
                    ? state.getInternalAllocationPercentage()
                    : 0;

            Integer currentProject = allocationRepository
                    .getActiveAllocationPercentage(resourceId, allocation.getAllocationStartDate());

            if (currentProject == null) currentProject = 0;

            int available = 100 - (internal + currentProject);

            int requested = allocation.getAllocationPercentage();

            // ✅ WITHIN CAPACITY
            if (requested <= available) {

                allocation.setApprovalStatus(ApprovalStatus.NOT_REQUIRED);
                allocation.setAllocationStatus(AllocationStatus.ACTIVE);
            }

            // ⚠ EXCEEDS → PENDING
            else {

                allocation.setApprovalStatus(ApprovalStatus.PENDING);
                allocation.setAllocationStatus(AllocationStatus.PLANNED);
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
        int existing = allocationRepository.findConflictingAllocationsForResources(request.getResourceId(), request.getAllocationStartDate(), request.getAllocationEndDate())
                .stream().filter(a -> !a.getAllocationId().equals(allocationId)).mapToInt(ResourceAllocation::getAllocationPercentage).sum();
        if (existing + request.getAllocationPercentage() > 100) {
            throw new ProjectExceptionHandler(HttpStatus.BAD_REQUEST, "OVER_ALLOCATION", "Exceeds resource capacity");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResponseEntity<ApiResponse<?>> closeAllocation(UUID allocationId, CloseAllocationDTO request) {
        Optional<ResourceAllocation> allocationOpt = allocationRepository.findById(allocationId);
        if (allocationOpt.isEmpty()) return ResponseEntity.notFound().build();

        ResourceAllocation allocation = allocationOpt.get();
        if (allocation.getAllocationStatus() == AllocationStatus.ENDED) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Allocation already ended", null));
        }

        LocalDate closureDate = request.getClosureDate() != null ? request.getClosureDate() : LocalDate.now();
        if (closureDate.isBefore(allocation.getAllocationStartDate())) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Closure date cannot be before start date", null));
        }

        allocation.setAllocationEndDate(closureDate);
        allocation.setAllocationStatus(AllocationStatus.ENDED);
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

    private void checkAndUpdateDemandFulfillment(UUID demandId) {
        try {
            Demand demand = demandRepository.findById(demandId).orElse(null);
            if (demand == null) return;

            List<ResourceAllocation> active = allocationRepository.findByDemand_DemandId(demandId).stream()
                    .filter(a -> a.getAllocationStatus() == AllocationStatus.ACTIVE).toList();

            if (active.size() >= demand.getResourcesRequired() && active.stream().allMatch(a -> a.getAllocationPercentage().equals(demand.getAllocationPercentage()))) {
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
                demand.setDemandStatus(DemandStatus.APPROVED);
                demandRepository.save(demand);
            }
        } catch (Exception e) {
            log.error("Demand fulfillment check failed for {}: {}", demandId, e.getMessage());
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
        dto.setCreatedBy(allocation.getCreatedBy());
        return dto;
    }

    private Integer calculateRemainingAllocationPercentage(Long resourceId, LocalDate startDate, LocalDate endDate, UUID currentId) {
        try {
            int total = allocationRepository.findConflictingAllocations(resourceId, startDate.minusDays(1), endDate.plusDays(1))
                    .stream().filter(a -> !a.getAllocationId().equals(currentId)).mapToInt(ResourceAllocation::getAllocationPercentage).sum();
            return Math.max(0, 130 - total);
        } catch (Exception e) {
            log.error("Error calculating remaining capacity for {}: {}", resourceId, e.getMessage());
            return 130;
        }
    }
}
