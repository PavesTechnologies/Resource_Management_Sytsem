package com.validator.allocation_validator;

import com.entity.allocation_entities.AllocationModification;
import com.entity.allocation_entities.ResourceAllocation;
import com.entity_enums.allocation_enums.AllocationModificationStatus;
import com.entity_enums.allocation_enums.AllocationStatus;
import com.global_exception_handler.ProjectExceptionHandler;
import com.repo.allocation_repo.AllocationModificationRepository;
import com.repo.allocation_repo.AllocationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Component
public class AllocationModificationValidator {

    @Autowired
    private AllocationRepository allocationRepository;

    @Autowired
    private AllocationModificationRepository modificationRepository;

    public void validateModificationRequest(UUID allocationId, Integer requestedPercentage, LocalDate effectiveDate) {
        validateAllocationExists(allocationId);
        ResourceAllocation allocation = allocationRepository.findById(allocationId).orElse(null);
        validateAllocationStatus(allocation);
        validateDemandBasedAllocation(allocation);
        validateAllocationPercentage(requestedPercentage);
        validateEffectiveDateRange(effectiveDate, allocation);
        validateEffectiveDateNotPast(effectiveDate);
        validateNoDuplicateModification(allocationId, effectiveDate);
        validateResourceUtilization(allocation, requestedPercentage, effectiveDate);
    }

    public void validateModificationDecision(AllocationModification modification, String decision, String rejectionReason) {
        validateModificationState(modification);
        if ("REJECT".equalsIgnoreCase(decision)) {
            validateRejectionReason(rejectionReason);
        }
    }

    public void validateModificationExecution(AllocationModification modification) {
        validateModificationApproved(modification);
        validateAllocationStillActive(modification.getAllocation());
        validateNoOverlappingAllocation(modification);
    }

    private void validateAllocationExists(UUID allocationId) {
        if (!allocationRepository.existsById(allocationId)) {
            throw ProjectExceptionHandler.badRequest("ALLOCATION_NOT_FOUND");
        }
    }

    private void validateAllocationStatus(ResourceAllocation allocation) {
        if (!AllocationStatus.ACTIVE.equals(allocation.getAllocationStatus())) {
            throw ProjectExceptionHandler.badRequest("INVALID_ALLOCATION_STATUS");
        }
    }

    private void validateDemandBasedAllocation(ResourceAllocation allocation) {
        if (allocation.getDemand() == null) {
            throw ProjectExceptionHandler.badRequest("MODIFICATION_ONLY_ALLOWED_FOR_DEMAND_ALLOCATIONS");
        }
    }

    private void validateAllocationPercentage(Integer requestedPercentage) {
        if (requestedPercentage == null) {
            throw ProjectExceptionHandler.badRequest("INVALID_ALLOCATION_PERCENTAGE");
        }
        if (requestedPercentage < 0 || requestedPercentage > 100) {
            throw ProjectExceptionHandler.badRequest("INVALID_ALLOCATION_PERCENTAGE");
        }
    }

    public boolean shouldTriggerRoleOff(Integer requestedPercentage) {
        return requestedPercentage != null && requestedPercentage == 0;
    }

    private void validateEffectiveDateRange(LocalDate effectiveDate, ResourceAllocation allocation) {
        if (effectiveDate.isBefore(allocation.getAllocationStartDate()) || 
            effectiveDate.isAfter(allocation.getAllocationEndDate())) {
            throw ProjectExceptionHandler.badRequest("EFFECTIVE_DATE_OUT_OF_RANGE");
        }
    }

    private void validateEffectiveDateNotPast(LocalDate effectiveDate) {
        if (effectiveDate.isBefore(LocalDate.now())) {
            throw ProjectExceptionHandler.badRequest("EFFECTIVE_DATE_IN_PAST");
        }
    }

    private void validateNoDuplicateModification(UUID allocationId, LocalDate effectiveDate) {
        List<AllocationModificationStatus> excludedStatuses = Arrays.asList(
                AllocationModificationStatus.CANCELLED, 
                AllocationModificationStatus.REJECTED
        );
        
        List<AllocationModification> duplicates = modificationRepository.findDuplicateModifications(
                allocationId, effectiveDate, excludedStatuses);
        
        if (!duplicates.isEmpty()) {
            throw ProjectExceptionHandler.conflict("MODIFICATION_ALREADY_EXISTS");
        }
    }

    private void validateResourceUtilization(ResourceAllocation allocation, Integer requestedPercentage, LocalDate effectiveDate) {
        // Get all allocations for the same resource that overlap with the effective date
        List<ResourceAllocation> overlappingAllocations = allocationRepository
                .findByResource_ResourceIdAndAllocationStartDateLessThanEqualAndAllocationEndDateGreaterThanEqual(
                        allocation.getResource().getResourceId(), effectiveDate);

        // Calculate total utilization excluding the current allocation
        int totalUtilization = overlappingAllocations.stream()
                .filter(a -> !a.getAllocationId().equals(allocation.getAllocationId()))
                .mapToInt(ResourceAllocation::getAllocationPercentage)
                .sum();

        if ((totalUtilization + requestedPercentage) > 100) {
            throw ProjectExceptionHandler.badRequest("RESOURCE_UTILIZATION_EXCEEDED");
        }
    }

    private void validateModificationState(AllocationModification modification) {
        if (!AllocationModificationStatus.REQUESTED.equals(modification.getStatus())) {
            throw ProjectExceptionHandler.badRequest("INVALID_MODIFICATION_STATE");
        }
    }

    private void validateRejectionReason(String rejectionReason) {
        if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
            throw ProjectExceptionHandler.badRequest("REJECTION_REASON_REQUIRED");
        }
    }

    private void validateModificationApproved(AllocationModification modification) {
        if (!AllocationModificationStatus.APPROVED.equals(modification.getStatus())) {
            throw ProjectExceptionHandler.badRequest("MODIFICATION_NOT_APPROVED");
        }
    }

    private void validateAllocationStillActive(ResourceAllocation allocation) {
        // Re-fetch allocation to ensure it's still active
        ResourceAllocation currentAllocation = allocationRepository.findById(allocation.getAllocationId())
                .orElseThrow(() -> ProjectExceptionHandler.notFound("ALLOCATION_NOT_FOUND"));
        
        if (!AllocationStatus.ACTIVE.equals(currentAllocation.getAllocationStatus())) {
            throw ProjectExceptionHandler.badRequest("ALLOCATION_NOT_ACTIVE");
        }
    }

    private void validateNoOverlappingAllocation(AllocationModification modification) {
        ResourceAllocation originalAllocation = modification.getAllocation();
        LocalDate effectiveDate = modification.getEffectiveDate();

        // Check if there are any allocations that would overlap with the new allocation segment
        List<ResourceAllocation> overlappingAllocations = allocationRepository
                .findConflictingAllocations(
                        originalAllocation.getResource().getResourceId(),
                        effectiveDate,
                        originalAllocation.getAllocationEndDate());

        // Exclude the original allocation from the check
        overlappingAllocations.removeIf(a -> a.getAllocationId().equals(originalAllocation.getAllocationId()));

        if (!overlappingAllocations.isEmpty()) {
            throw ProjectExceptionHandler.conflict("ALLOCATION_OVERLAP_DETECTED");
        }
    }

    public void validateUserCanCancelModification(AllocationModification modification, String username) {
        if (!modification.getRequestedBy().equals(username)) {
            throw ProjectExceptionHandler.badRequest("USER_CANNOT_CANCEL_MODIFICATION");
        }
    }
}
