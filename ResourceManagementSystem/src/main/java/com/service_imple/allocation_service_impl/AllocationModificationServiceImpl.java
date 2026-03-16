package com.service_imple.allocation_service_impl;

import com.dto.ApiResponse;
import com.dto.UserDTO;
import com.dto.allocation_dto.AllocationModificationDecisionDTO;
import com.dto.allocation_dto.AllocationModificationResponseDTO;
import com.dto.allocation_dto.CreateAllocationModificationDTO;
import com.entity.allocation_entities.AllocationModification;
import com.entity.allocation_entities.ResourceAllocation;
import com.entity.project_entities.Project;
import com.entity.resource_entities.Resource;
import com.entity_enums.allocation_enums.AllocationModificationStatus;
import com.entity_enums.allocation_enums.AllocationStatus;
import com.global_exception_handler.ProjectExceptionHandler;
import com.repo.allocation_repo.AllocationModificationRepository;
import com.repo.allocation_repo.AllocationRepository;
import com.repo.project_repo.ProjectRepository;
import com.repo.resource_repo.ResourceRepository;
import com.service_interface.allocation_service_interface.AllocationModificationService;
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
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AllocationModificationServiceImpl implements AllocationModificationService {

    @Autowired
    private AllocationModificationRepository modificationRepository;

    @Autowired
    private AllocationRepository allocationRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private AllocationModificationValidator validator;

    
    @Override
    @Transactional
    public ResponseEntity<ApiResponse<?>> createModification(CreateAllocationModificationDTO dto, UserDTO userDTO) {
        try {
            // Check if this should trigger Role-Off workflow
            if (validator.shouldTriggerRoleOff(dto.getRequestedAllocationPercentage())) {
                return triggerRoleOffProcess(dto, userDTO);
            }

            // Comprehensive validation
            validator.validateModificationRequest(
                    dto.getAllocationId(), 
                    dto.getRequestedAllocationPercentage(), 
                    dto.getEffectiveDate()
            );

            // Get allocation for modification creation
            ResourceAllocation allocation = allocationRepository.findById(dto.getAllocationId())
                    .orElseThrow(() -> ProjectExceptionHandler.notFound("Allocation not found"));

            // Create modification request
            AllocationModification modification = new AllocationModification();
            modification.setAllocation(allocation);
            modification.setCurrentAllocationPercentage(allocation.getAllocationPercentage());
            modification.setRequestedAllocationPercentage(dto.getRequestedAllocationPercentage());
            modification.setEffectiveDate(dto.getEffectiveDate());
            modification.setReason(dto.getReason());
            modification.setStatus(AllocationModificationStatus.REQUESTED);
            modification.setRequestedBy(userDTO.getName());

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
    public ResponseEntity<ApiResponse<?>> executeModification(UUID modificationId) {
        try {
            AllocationModification modification = modificationRepository.findById(modificationId)
                    .orElseThrow(() -> ProjectExceptionHandler.notFound("Modification request not found"));

            // Comprehensive execution validation
            validator.validateModificationExecution(modification);

            ResourceAllocation originalAllocation = modification.getAllocation();
            LocalDate effectiveDate = modification.getEffectiveDate();
            Integer newPercentage = modification.getRequestedAllocationPercentage();

            // Update original allocation end date
            originalAllocation.setAllocationEndDate(effectiveDate.minusDays(1));
            allocationRepository.save(originalAllocation);

            // Create new allocation with modified percentage
            ResourceAllocation newAllocation = new ResourceAllocation();
            newAllocation.setAllocationId(UUID.randomUUID());
            newAllocation.setResource(originalAllocation.getResource());
            newAllocation.setProject(originalAllocation.getProject());
            newAllocation.setDemand(originalAllocation.getDemand());
            newAllocation.setAllocationStartDate(effectiveDate);
            newAllocation.setAllocationEndDate(originalAllocation.getAllocationEndDate());
            newAllocation.setAllocationPercentage(newPercentage);
            newAllocation.setAllocationStatus(AllocationStatus.ACTIVE);
            newAllocation.setCreatedBy(modification.getApprovedBy());

            allocationRepository.save(newAllocation);

            return ResponseEntity.ok(new ApiResponse<>(true, "Modification executed successfully", null));

        } catch (ProjectExceptionHandler e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Error executing modification: " + e.getMessage(), null));
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
        return dto;
    }
}
