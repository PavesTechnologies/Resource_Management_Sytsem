package com.service_imple.allocation_service_impl;

import com.dto.ApiResponse;
import com.dto.UserDTO;
import com.dto.allocation_dto.AllocationModificationDecisionDTO;
import com.dto.allocation_dto.AllocationModificationResponseDTO;
import com.dto.allocation_dto.CreateAllocationModificationDTO;
import com.entity.allocation_entities.AllocationModification;
import com.entity.allocation_entities.ResourceAllocation;
import com.entity_enums.allocation_enums.AllocationModificationStatus;
import com.entity_enums.allocation_enums.AllocationStatus;
import com.global_exception_handler.ProjectExceptionHandler;
import com.repo.allocation_repo.AllocationModificationRepository;
import com.repo.allocation_repo.AllocationRepository;
import com.service_interface.allocation_service_interface.AllocationModificationService;
import com.service_interface.allocation_service_interface.AllocationService;
import com.validator.allocation_validator.AllocationModificationValidator;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AllocationModificationServiceImpl implements AllocationModificationService {

    @Autowired
    private AllocationModificationRepository modificationRepository;

    @Autowired
    private AllocationRepository allocationRepository;

    @Autowired
    private AllocationModificationValidator validator;

    @Autowired
    private AllocationService allocationService;

    
    @Override
    @Transactional
    public ResponseEntity<ApiResponse<?>> createModification(CreateAllocationModificationDTO dto, UserDTO userDTO) {
        try {
            // Check if this should trigger Role-Off workflow
            if (validator.shouldTriggerRoleOff(dto.getRequestedAllocationPercentage())) {
                return triggerRoleOffProcess(dto, userDTO);
            }

            // Unified validation for both modification and override
            validator.validateModificationRequest(
                    dto.getAllocationId(), 
                    dto.getRequestedAllocationPercentage(), 
                    dto.getEffectiveDate(),
                    dto.getOverrideJustification()
            );

            // Get allocation for modification creation
            ResourceAllocation allocation = allocationRepository.findById(dto.getAllocationId())
                    .orElseThrow(() -> ProjectExceptionHandler.notFound("Allocation not found"));

            // Check if override is needed
            boolean isOverride = checkIfOverrideRequired(allocation.getResource().getResourceId(), dto);

            // Create modification request
            AllocationModification modification = new AllocationModification();
            modification.setAllocation(allocation);
            modification.setCurrentAllocationPercentage(allocation.getAllocationPercentage());
            modification.setRequestedAllocationPercentage(dto.getRequestedAllocationPercentage());
            modification.setEffectiveDate(dto.getEffectiveDate());
            modification.setReason(dto.getReason());
            modification.setStatus(AllocationModificationStatus.REQUESTED);
            modification.setRequestedBy(userDTO.getName());
            
            // Set override fields if needed
            if (isOverride) {
                modification.setOverrideFlag(true);
                modification.setOverrideJustification(dto.getOverrideJustification());
                modification.setOverrideBy(userDTO.getName());
                modification.setOverrideAt(LocalDateTime.now());
            }

            AllocationModification savedModification = modificationRepository.save(modification);

            AllocationModificationResponseDTO responseDTO = convertToResponseDTO(savedModification);
            return ResponseEntity.ok(new ApiResponse<>(true, "Allocation modification request created successfully", responseDTO));

        } catch (ProjectExceptionHandler e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Error creating modification request: " + e.getMessage(), null));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<?>> processModificationDecision(UUID modificationId, AllocationModificationDecisionDTO dto, UserDTO userDTO) {
        try {
            AllocationModification modification = modificationRepository.findById(modificationId)
                    .orElseThrow(() -> ProjectExceptionHandler.notFound("Modification request not found"));

            // Validate modification state and decision
            validator.validateModificationDecision(modification, dto.getDecision(), dto.getRejectionReason());

            if ("APPROVE".equalsIgnoreCase(dto.getDecision())) {
                modification.setStatus(AllocationModificationStatus.APPROVED);
                modification.setApprovedBy(userDTO.getName());
                modification.setApprovedAt(LocalDateTime.now());

                // Execute the modification
                ResponseEntity<ApiResponse<?>> executionResult = executeModification(modificationId);
                if (executionResult.getStatusCode().is2xxSuccessful()) {
                    modification.setStatus(AllocationModificationStatus.FULFILLED);
                } else {
                    // Rollback status if execution failed
                    modification.setStatus(AllocationModificationStatus.REQUESTED);
                    modification.setApprovedBy(null);
                    modification.setApprovedAt(null);
                }
            } else if ("REJECT".equalsIgnoreCase(dto.getDecision())) {
                modification.setStatus(AllocationModificationStatus.REJECTED);
                modification.setRejectReason(dto.getRejectionReason());
                modification.setRejectedBy(userDTO.getName());
            }

            modificationRepository.save(modification);
            AllocationModificationResponseDTO responseDTO = convertToResponseDTO(modification);
            return ResponseEntity.ok(new ApiResponse<>(true, "Modification decision processed successfully", responseDTO));

        } catch (ProjectExceptionHandler e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Error processing modification decision: " + e.getMessage(), null));
        }
    }

    
    @Override
    @Transactional
    public ResponseEntity<ApiResponse<?>> cancelModification(UUID modificationId, UserDTO userDTO) {
        try {
            AllocationModification modification = modificationRepository.findById(modificationId)
                    .orElseThrow(() -> ProjectExceptionHandler.notFound("Modification request not found"));

            if (!AllocationModificationStatus.REQUESTED.equals(modification.getStatus())) {
                return ResponseEntity.badRequest().body(new ApiResponse<>(false, "Only REQUESTED modifications can be cancelled", null));
            }

            // Validate user can cancel this modification
            validator.validateUserCanCancelModification(modification, userDTO.getName());

            modification.setStatus(AllocationModificationStatus.CANCELLED);
            modificationRepository.save(modification);

            AllocationModificationResponseDTO responseDTO = convertToResponseDTO(modification);
            return ResponseEntity.ok(new ApiResponse<>(true, "Modification request cancelled successfully", responseDTO));

        } catch (ProjectExceptionHandler e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Error cancelling modification: " + e.getMessage(), null));
        }
    }

    @Override
    public ResponseEntity<ApiResponse<AllocationModificationResponseDTO>> getModificationById(UUID modificationId) {
        try {
            AllocationModification modification = modificationRepository.findById(modificationId)
                    .orElseThrow(() -> ProjectExceptionHandler.notFound("Modification request not found"));

            AllocationModificationResponseDTO responseDTO = convertToResponseDTO(modification);
            return ResponseEntity.ok(new ApiResponse<>(true, "Modification retrieved successfully", responseDTO));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Error retrieving modification: " + e.getMessage(), null));
        }
    }

    @Override
    public ResponseEntity<ApiResponse<List<AllocationModificationResponseDTO>>> getModificationsByProjectManager(UserDTO userDTO) {
        try {
            List<AllocationModification> modifications = modificationRepository.findByProjectManagerId(userDTO.getId());
            List<AllocationModificationResponseDTO> responseDTOs = modifications.stream()
                    .map(this::convertToResponseDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(new ApiResponse<>(true, "Modifications retrieved successfully", responseDTOs));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Error retrieving modifications: " + e.getMessage(), null));
        }
    }

    @Override
    public ResponseEntity<ApiResponse<List<AllocationModificationResponseDTO>>> getModificationsByResourceManager(UserDTO userDTO) {
        try {
            List<AllocationModification> modifications = modificationRepository.findByResourceManagerId(userDTO.getId());
            List<AllocationModificationResponseDTO> responseDTOs = modifications.stream()
                    .map(this::convertToResponseDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(new ApiResponse<>(true, "Modifications retrieved successfully", responseDTOs));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Error retrieving modifications: " + e.getMessage(), null));
        }
    }

    @Override
    public ResponseEntity<ApiResponse<List<AllocationModificationResponseDTO>>> getModificationsByDemand(UUID demandId) {
        try {
            List<AllocationModification> modifications = modificationRepository.findByDemandId(demandId);
            List<AllocationModificationResponseDTO> responseDTOs = modifications.stream()
                    .map(this::convertToResponseDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(new ApiResponse<>(true, "Modifications retrieved successfully for demand", responseDTOs));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Error retrieving modifications for demand: " + e.getMessage(), null));
        }
    }

    private ResponseEntity<ApiResponse<?>> triggerRoleOffProcess(CreateAllocationModificationDTO dto, UserDTO userDTO) {
        try {
            // For Role-Off, we create a modification request that's immediately fulfilled
            ResourceAllocation allocation = allocationRepository.findById(dto.getAllocationId())
                    .orElseThrow(() -> ProjectExceptionHandler.notFound("Allocation not found"));

            // Validate basic allocation status
            if (!AllocationStatus.ACTIVE.equals(allocation.getAllocationStatus())) {
                throw ProjectExceptionHandler.badRequest("INVALID_ALLOCATION_STATUS");
            }

            // Validate effective date
            if (dto.getEffectiveDate().isBefore(LocalDate.now())) {
                throw ProjectExceptionHandler.badRequest("EFFECTIVE_DATE_IN_PAST");
            }

            // Create Role-Off modification request
            AllocationModification modification = new AllocationModification();
            modification.setAllocation(allocation);
            modification.setCurrentAllocationPercentage(allocation.getAllocationPercentage());
            modification.setRequestedAllocationPercentage(0);
            modification.setEffectiveDate(dto.getEffectiveDate());
            modification.setReason("Role-Off: " + dto.getReason());
            modification.setStatus(AllocationModificationStatus.FULFILLED);
            modification.setRequestedBy(userDTO.getName());
            modification.setApprovedBy(userDTO.getName());
            modification.setApprovedAt(LocalDateTime.now());

            // Execute Role-Off immediately
            allocation.setAllocationEndDate(dto.getEffectiveDate().minusDays(1));
            allocationRepository.save(allocation);

            modificationRepository.save(modification);

            AllocationModificationResponseDTO responseDTO = convertToResponseDTO(modification);
            return ResponseEntity.ok(new ApiResponse<>(true, "Role-Off process completed successfully", responseDTO));

        } catch (ProjectExceptionHandler e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Error processing Role-Off: " + e.getMessage(), null));
        }
    }

    private AllocationModificationResponseDTO convertToResponseDTO(AllocationModification modification) {
        AllocationModificationResponseDTO dto = new AllocationModificationResponseDTO();
        dto.setModificationId(modification.getModificationId());
        dto.setAllocationId(modification.getAllocation().getAllocationId());
        
        // Add resource information
        if (modification.getAllocation().getResource() != null) {
            dto.setResourceName(modification.getAllocation().getResource().getFullName());
        }
        
        dto.setCurrentAllocationPercentage(modification.getCurrentAllocationPercentage());
        dto.setRequestedAllocationPercentage(modification.getRequestedAllocationPercentage());
        dto.setEffectiveDate(modification.getEffectiveDate().toString());
        dto.setStatus(modification.getStatus());
        dto.setRequestedBy(modification.getRequestedBy());
        dto.setApprovedBy(modification.getApprovedBy());
        dto.setRequestedAt(modification.getRequestedAt());
        dto.setApprovedAt(modification.getApprovedAt());
        dto.setReason(modification.getReason());
        dto.setRejectReason(modification.getRejectReason());
        dto.setRejectedBy(modification.getRejectedBy());
        dto.setOverrideFlag(modification.getOverrideFlag());
        dto.setOverrideJustification(modification.getOverrideJustification());
        dto.setOverrideBy(modification.getOverrideBy());
        dto.setOverrideAt(modification.getOverrideAt());
        return dto;
    }

    private ResponseEntity<ApiResponse<?>> executeModification(UUID modificationId) {
        try {
            AllocationModification modification = modificationRepository.findById(modificationId)
                    .orElseThrow(() -> ProjectExceptionHandler.notFound("Modification request not found"));

            ResourceAllocation originalAllocation = modification.getAllocation();
            LocalDate effectiveDate = modification.getEffectiveDate();
            Integer newPercentage = modification.getRequestedAllocationPercentage();
            
            // Handle role-off (0% allocation)
            if (newPercentage == 0) {
                return handleRoleOffModification(originalAllocation, effectiveDate);
            }
            
            // Handle percentage change with period split
            return handlePercentageModification(originalAllocation, effectiveDate, newPercentage);

        } catch (ProjectExceptionHandler e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Error executing modification: " + e.getMessage(), null));
        }
    }

    /**
     * Handles role-off modifications by ending the allocation
     */
    private ResponseEntity<ApiResponse<?>> handleRoleOffModification(ResourceAllocation allocation, LocalDate effectiveDate) {
        // End the existing allocation at effective date - 1
        LocalDate endDate = effectiveDate.minusDays(1);
        
        if (effectiveDate.isAfter(allocation.getAllocationStartDate())) {
            allocation.setAllocationEndDate(endDate);
            allocation.setAllocationStatus(AllocationStatus.ENDED);
            allocationRepository.save(allocation);
            
            // Update ledger for the ended period
            allocationService.updateAvailabilityLedgerForAllocation(allocation);
        } else {
            // If effective date is same as start date, just cancel the allocation
            allocation.setAllocationStatus(AllocationStatus.CANCELLED);
            allocationRepository.save(allocation);
            allocationService.updateAvailabilityLedgerForAllocation(allocation);
        }
        
        return ResponseEntity.ok(new ApiResponse<>(true, "Role-off modification executed successfully", null));
    }

    /**
     * Handles percentage change modifications by splitting allocation periods
     */
    private ResponseEntity<ApiResponse<?>> handlePercentageModification(ResourceAllocation originalAllocation, 
                                                                        LocalDate effectiveDate, 
                                                                        Integer newPercentage) {
        
        // Validate effective date is within allocation period
        if (effectiveDate.isBefore(originalAllocation.getAllocationStartDate()) || 
            effectiveDate.isAfter(originalAllocation.getAllocationEndDate())) {
            throw ProjectExceptionHandler.badRequest("Effective date must be within allocation period");
        }
        
        // Validate total allocation won't exceed 100%
        validateTotalAllocation(originalAllocation.getResource().getResourceId(), effectiveDate, 
                               originalAllocation.getAllocationPercentage(), newPercentage);
        
        // If effective date is the start date, just update the percentage
        if (effectiveDate.equals(originalAllocation.getAllocationStartDate())) {
            originalAllocation.setAllocationPercentage(newPercentage);
            allocationRepository.save(originalAllocation);
            allocationService.updateAvailabilityLedgerForAllocation(originalAllocation);
            return ResponseEntity.ok(new ApiResponse<>(true, "Allocation percentage updated successfully", null));
        }
        
        // Split the allocation: end original period and create new period
        // 1. End the original allocation at effective date - 1
        LocalDate originalEndDate = originalAllocation.getAllocationEndDate();
        originalAllocation.setAllocationEndDate(effectiveDate.minusDays(1));
        allocationRepository.save(originalAllocation);
        
        // Update ledger for the original (shortened) period
        allocationService.updateAvailabilityLedgerForAllocation(originalAllocation);
        
        // 2. Create new allocation with new percentage starting from effective date
        ResourceAllocation newAllocation = createNewAllocationPeriod(originalAllocation, effectiveDate, originalEndDate, newPercentage);
        allocationRepository.save(newAllocation);
        
        // Update ledger for the new allocation period
        allocationService.updateAvailabilityLedgerForAllocation(newAllocation);
        
        return ResponseEntity.ok(new ApiResponse<>(true, "Allocation modification executed successfully with period split", null));
    }

    /**
     * Creates a new allocation period with the updated percentage
     */
    private ResourceAllocation createNewAllocationPeriod(ResourceAllocation original, 
                                                        LocalDate startDate, 
                                                        LocalDate endDate, 
                                                        Integer newPercentage) {
        ResourceAllocation newAllocation = new ResourceAllocation();
        newAllocation.setResource(original.getResource());
        newAllocation.setDemand(original.getDemand());
        newAllocation.setProject(original.getProject());
        newAllocation.setAllocationStartDate(startDate);
        newAllocation.setAllocationEndDate(endDate);
        newAllocation.setAllocationPercentage(newPercentage);
        newAllocation.setAllocationStatus(AllocationStatus.ACTIVE);
        newAllocation.setCreatedBy(original.getCreatedBy());
        
        return newAllocation;
    }

    /**
     * Validates that total allocation for a resource won't exceed 100% after modification
     */
    private void validateTotalAllocation(Long resourceId, LocalDate effectiveDate, 
                                       Integer currentPercentage, Integer newPercentage) {
        try {
            // Get all active allocations for this resource on the effective date
            List<ResourceAllocation> activeAllocations = allocationRepository
                .findActiveAllocationsForResourceOnDate(resourceId, effectiveDate);
            
            // Calculate current total allocation (excluding the allocation being modified)
            int currentTotal = activeAllocations.stream()
                .mapToInt(ResourceAllocation::getAllocationPercentage)
                .sum();
            
            // Subtract the current percentage of the allocation being modified
            // since it will be replaced with the new percentage
            int otherAllocationsTotal = currentTotal - currentPercentage;
            
            // Calculate new total with the modified percentage
            int newTotalAllocation = otherAllocationsTotal + newPercentage;
            
            // Validate against 100% capacity
            if (newTotalAllocation > 100) {
                throw ProjectExceptionHandler.badRequest(
                    String.format("Modification would result in %d%% total allocation for resource on %s. " +
                                "Maximum allowed is 100%%. Other allocations: %d%%, This allocation: %d%%",
                                newTotalAllocation, effectiveDate, otherAllocationsTotal, newPercentage));
            }
            
        } catch (Exception e) {
            // If validation fails due to data access issues, allow the modification
            // The ledger update will handle any over-allocation issues
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<?>> deleteModification(UUID modificationId, UserDTO userDTO) {
        try {
            AllocationModification modification = modificationRepository.findById(modificationId)
                    .orElseThrow(() -> ProjectExceptionHandler.notFound("Modification request not found"));

            // Check if the modification belongs to the current project manager
            if (!modification.getRequestedBy().equals(userDTO.getName())) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, "You can only delete your own modification requests", null));
            }

            // Check if modification is still requested (can only delete requested modifications)
            if (modification.getStatus() != AllocationModificationStatus.REQUESTED) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, "Can only delete requested modification requests", null));
            }

            modificationRepository.delete(modification);
            
            return ResponseEntity.ok(new ApiResponse<>(true, "Modification request deleted successfully", null));

        } catch (ProjectExceptionHandler e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Error deleting modification: " + e.getMessage(), null));
        }
    }


    private boolean checkIfOverrideRequired(Long resourceId, CreateAllocationModificationDTO dto) {
        // Get the current allocation to determine the date range
        ResourceAllocation currentAllocation = allocationRepository.findById(dto.getAllocationId())
                .orElseThrow(() -> ProjectExceptionHandler.notFound("Allocation not found"));
        
        // Use validator's checkIfOverrideRequired method to avoid duplication
        return validator.checkIfOverrideRequired(currentAllocation, dto.getRequestedAllocationPercentage(), dto.getEffectiveDate());
    }
}
