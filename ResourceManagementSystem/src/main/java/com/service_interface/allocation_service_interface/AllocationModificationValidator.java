package com.service_interface.allocation_service_interface;

import com.entity.allocation_entities.AllocationModification;
import com.entity.allocation_entities.ResourceAllocation;
import com.entity_enums.allocation_enums.AllocationModificationStatus;
import com.entity_enums.allocation_enums.AllocationStatus;
import com.global_exception_handler.ProjectExceptionHandler;
import com.repo.allocation_repo.AllocationModificationRepository;
import com.repo.allocation_repo.AllocationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Unified Allocation Change Validator
 * 
 * This validator handles both allocation modifications and overrides in a single, 
 * non-redundant way. The system automatically determines if an override is needed
 * based on business rules, eliminating separate validation flows.
 */
@Component
public class AllocationModificationValidator {

    @Autowired
    private AllocationRepository allocationRepository;

    @Autowired
    private AllocationModificationRepository modificationRepository;

    /**
     * Unified validation for all allocation changes
     * 
     * @param allocationId Target allocation
     * @param requestedPercentage New percentage (can be over 100%)
     * @param effectiveDate When change takes effect
     * @param overrideEndDate When override ends (optional)
     * @param reason Reason for the change (used for both normal and override cases)
     */
    public void validateAllocationChange(UUID allocationId, Integer requestedPercentage, 
                                       LocalDate effectiveDate, LocalDate overrideEndDate,
                                       String reason) {
        // Basic validations
        validateAllocationExists(allocationId);
        ResourceAllocation allocation = allocationRepository.findById(allocationId).orElse(null);
        validateAllocationStatus(allocation);
        validateDemandBasedAllocation(allocation);
        
        // Determine if override is needed
        boolean overrideRequired = checkIfOverrideRequired(allocation, requestedPercentage, effectiveDate);
        
        // Unified percentage validation - allows over 100% with proper reason
        validateAllocationPercentage(requestedPercentage, overrideRequired, reason);
        
        // Date validations
        validateEffectiveDateRange(effectiveDate, allocation);
        validateEffectiveDateNotPast(effectiveDate);
        validateNoDuplicateModification(allocationId, effectiveDate);
        
        // Override duration validation (only if override is needed)
        if (overrideRequired) {
            // Check if total allocation will exceed 100%
            Long resourceId = allocation.getResource().getResourceId();
            List<ResourceAllocation> activeAllocations = allocationRepository
                    .findActiveAllocationsForResourceOnDate(resourceId, effectiveDate);
            
            int currentTotalAllocation = activeAllocations.stream()
                    .filter(a -> !a.getAllocationId().equals(allocation.getAllocationId()))
                    .mapToInt(ResourceAllocation::getAllocationPercentage)
                    .sum();
            
            // Calculate new total with requested allocation
            int newTotalAllocation = currentTotalAllocation + requestedPercentage;
            
            // Check if we're reducing an over-allocation
            boolean isCurrentlyOverAllocated = allocation.getAllocationPercentage() > 100;
            boolean isReducingToNormal = isCurrentlyOverAllocated && requestedPercentage <= 100;
            
            // Make overrideEndDate mandatory only when total allocation > 100% AND we're not reducing an over-allocation
            if (newTotalAllocation > 100 && !isReducingToNormal) {
                LocalDate allocationEndDate = allocation.getAllocationEndDate();
                long daysToAllocationEnd = java.time.temporal.ChronoUnit.DAYS.between(effectiveDate, allocationEndDate);
                
                // If allocation ends within 14 days, automatically use allocation end date as override end date
                if (daysToAllocationEnd <= 14) {
                    if (overrideEndDate != null && !overrideEndDate.equals(allocationEndDate)) {
                        throw ProjectExceptionHandler.badRequest("OVERRIDE_END_DATE_SHOULD_BE_ALLOCATION_END_DATE");
                    }
                    // Set override end date to allocation end date automatically
                    overrideEndDate = allocationEndDate;
                } else {
                    // For longer allocations, override end date is mandatory
                    if (overrideEndDate == null) {
                        throw ProjectExceptionHandler.badRequest("OVERRIDE_END_DATE_REQUIRED_FOR_OVER_ALLOCATION");
                    }
                }
                validateOverrideDuration(effectiveDate, overrideEndDate, allocation, newTotalAllocation);
            } else if (overrideEndDate != null) {
                // For normal allocations (≤100%) or when reducing over-allocation, overrideEndDate is optional but if provided, validate it
                validateOverrideDuration(effectiveDate, overrideEndDate, allocation, newTotalAllocation);
            }
        }
        
        // Override reason validation (only if needed)
        if (overrideRequired && (reason == null || reason.trim().isEmpty())) {
            throw ProjectExceptionHandler.badRequest("OVERRIDE_JUSTIFICATION_REQUIRED");
        }
    }

    /**
     * Validate override duration parameters
     */
    private void validateOverrideDuration(LocalDate effectiveDate, LocalDate overrideEndDate, ResourceAllocation allocation, int newTotalAllocation) {
        // Override end date must be after effective date
        if (!overrideEndDate.isAfter(effectiveDate)) {
            throw ProjectExceptionHandler.badRequest("OVERRIDE_END_DATE_MUST_BE_AFTER_EFFECTIVE_DATE");
        }
        
        // Override end date cannot exceed allocation end date
        if (overrideEndDate.isAfter(allocation.getAllocationEndDate())) {
            throw ProjectExceptionHandler.badRequest("OVERRIDE_END_DATE_CANNOT_EXCEED_ALLOCATION_END_DATE");
        }
        
        // Override duration must be reasonable (not too short)
        long overrideDuration = java.time.temporal.ChronoUnit.DAYS.between(effectiveDate, overrideEndDate);
        if (overrideDuration < 7) { // Less than 7 days
            throw ProjectExceptionHandler.badRequest("OVERRIDE_DURATION_TOO_SHORT");
        }
        
        // If total allocation > 100%, override duration must be 14 days or less
        if (newTotalAllocation > 100) {
            if (overrideDuration > 14) {
                throw ProjectExceptionHandler.badRequest("OVERRIDE_DURATION_EXCEEDS_14_DAYS_FOR_OVER_ALLOCATION");
            }
        }
        // For normal allocations (≤100%), no maximum duration limit - only minimum 7 days
    }

    /**
     * Legacy method for backward compatibility - redirects to unified validation
     */
    public void validateModificationRequest(UUID allocationId, Integer requestedPercentage, 
                                       LocalDate effectiveDate, String reason) {
        validateAllocationChange(allocationId, requestedPercentage, effectiveDate, null, reason);
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

    private void validateAllocationPercentage(Integer requestedPercentage, boolean overrideRequired, String reason) {
        if (requestedPercentage == null) {
            throw ProjectExceptionHandler.badRequest("INVALID_ALLOCATION_PERCENTAGE");
        }
        if (requestedPercentage < 0) {
            throw ProjectExceptionHandler.badRequest("INVALID_ALLOCATION_PERCENTAGE");
        }
        
        // Allow percentages over 100% only if override is provided and justified
        if (requestedPercentage > 100 && (!overrideRequired || reason == null || reason.trim().isEmpty())) {
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
        // This validation is now handled in checkIfOverrideRequired method
        // No need to duplicate here
    }
    
    public boolean checkIfOverrideRequired(ResourceAllocation allocation, Integer requestedPercentage, LocalDate effectiveDate) {
        // Get all allocations for the same resource that overlap with the effective date
        List<ResourceAllocation> overlappingAllocations = allocationRepository
                .findByResource_ResourceIdAndAllocationStartDateLessThanEqualAndAllocationEndDateGreaterThanEqual(
                        allocation.getResource().getResourceId(), effectiveDate);

        // Calculate total utilization excluding the current allocation
        int totalUtilization = overlappingAllocations.stream()
                .filter(a -> !a.getAllocationId().equals(allocation.getAllocationId()))
                .mapToInt(ResourceAllocation::getAllocationPercentage)
                .sum();

        // Calculate new total with requested allocation
        int newTotalAllocation = totalUtilization + requestedPercentage;
        
        // Check if current allocation is already an over-allocation (>100%)
        boolean isCurrentlyOverAllocated = allocation.getAllocationPercentage() > 100;
        
        // If current allocation is over-allocated and new allocation is <= 100%, 
        // then we're fixing an over-allocation, not creating one
        if (isCurrentlyOverAllocated && requestedPercentage <= 100) {
            return false;
        }
        
        // Return true if override is needed (utilization would exceed 100%)
        return newTotalAllocation > 100;
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
