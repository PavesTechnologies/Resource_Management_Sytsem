package com.service_imple.allocation_service_imple;

import com.dto.AllocationRequestDTO;
import com.dto.ApiResponse;
import com.entity.allocation_entities.ResourceAllocation;
import com.entity_enums.allocation_enums.AllocationStatus;
import com.repo.allocation_repo.AllocationRepository;
import com.repo.resource_repo.ResourceRepository;
import com.repo.DemandRepository;
import com.repo.project_repo.ProjectRepository;
import com.service_interface.allocation_service_interface.AllocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AllocationServiceImple implements AllocationService {

    private final AllocationRepository allocationRepository;
    private final ResourceRepository resourceRepository;
    private final DemandRepository demandRepository;
    private final ProjectRepository projectRepository;

    @Override
    public ResponseEntity<ApiResponse> assignAllocation(AllocationRequestDTO allocationRequest) {
        try {
            // Validate resource exists
            if (!resourceRepository.existsById(allocationRequest.getResourceId())) {
                return ResponseEntity.badRequest().body(
                    new ApiResponse<>(false, "Resource not found with ID: " + allocationRequest.getResourceId(), null)
                );
            }

            // Validate demand or project exists
            if (allocationRequest.getDemandId() != null) {
                if (!demandRepository.existsById(allocationRequest.getDemandId())) {
                    return ResponseEntity.badRequest().body(
                        new ApiResponse<>(false, "Demand not found with ID: " + allocationRequest.getDemandId(), null)
                    );
                }
            } else if (allocationRequest.getProjectId() != null) {
                if (!projectRepository.existsById(allocationRequest.getProjectId())) {
                    return ResponseEntity.badRequest().body(
                        new ApiResponse<>(false, "Project not found with ID: " + allocationRequest.getProjectId(), null)
                    );
                }
            }

            // Create allocation entity
            ResourceAllocation allocation = new ResourceAllocation();
            allocation.setAllocationId(UUID.randomUUID());
            allocation.setResource(resourceRepository.findById(allocationRequest.getResourceId()).orElse(null));
            allocation.setDemand(allocationRequest.getDemandId() != null ? 
                demandRepository.findById(allocationRequest.getDemandId()).orElse(null) : null);
            allocation.setProject(allocationRequest.getProjectId() != null ? 
                projectRepository.findById(allocationRequest.getProjectId()).orElse(null) : null);
            allocation.setAllocationStartDate(allocationRequest.getAllocationStartDate());
            allocation.setAllocationEndDate(allocationRequest.getAllocationEndDate());
            allocation.setAllocationPercentage(allocationRequest.getAllocationPercentage());
            allocation.setAllocationStatus(allocationRequest.getAllocationStatus());
            allocation.setCreatedBy(allocationRequest.getCreatedBy());
            allocation.setCreatedAt(LocalDateTime.now());

            ResourceAllocation savedAllocation = allocationRepository.save(allocation);
            
            log.info("Successfully created allocation {} for resource {}", 
                    savedAllocation.getAllocationId(), allocationRequest.getResourceId());

            return ResponseEntity.ok(
                new ApiResponse<>(true, "Allocation created successfully", savedAllocation)
            );

        } catch (Exception e) {
            log.error("Error creating allocation: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                new ApiResponse<>(false, "Error creating allocation: " + e.getMessage(), null)
            );
        }
    }

    @Override
    public ResponseEntity<ApiResponse> getAllocationById(UUID allocationId) {
        try {
            Optional<ResourceAllocation> allocation = allocationRepository.findById(allocationId);
            
            if (allocation.isPresent()) {
                return ResponseEntity.ok(
                    new ApiResponse<>(true, "Allocation found", allocation.get())
                );
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error retrieving allocation {}: {}", allocationId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                new ApiResponse<>(false, "Error retrieving allocation: " + e.getMessage(), null)
            );
        }
    }

    @Override
    public ResponseEntity<ApiResponse> updateAllocation(UUID allocationId, AllocationRequestDTO allocationRequest) {
        try {
            Optional<ResourceAllocation> existingAllocation = allocationRepository.findById(allocationId);
            
            if (!existingAllocation.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            ResourceAllocation allocation = existingAllocation.get();
            
            // Update fields
            allocation.setAllocationStartDate(allocationRequest.getAllocationStartDate());
            allocation.setAllocationEndDate(allocationRequest.getAllocationEndDate());
            allocation.setAllocationPercentage(allocationRequest.getAllocationPercentage());
            allocation.setAllocationStatus(allocationRequest.getAllocationStatus());

            ResourceAllocation updatedAllocation = allocationRepository.save(allocation);
            
            log.info("Successfully updated allocation {}", allocationId);

            return ResponseEntity.ok(
                new ApiResponse<>(true, "Allocation updated successfully", updatedAllocation)
            );

        } catch (Exception e) {
            log.error("Error updating allocation {}: {}", allocationId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                new ApiResponse<>(false, "Error updating allocation: " + e.getMessage(), null)
            );
        }
    }

    @Override
    public ResponseEntity<ApiResponse> cancelAllocation(UUID allocationId, String cancelledBy) {
        try {
            Optional<ResourceAllocation> existingAllocation = allocationRepository.findById(allocationId);
            
            if (!existingAllocation.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            ResourceAllocation allocation = existingAllocation.get();
            allocation.setAllocationStatus(AllocationStatus.CANCELLED);
            
            ResourceAllocation cancelledAllocation = allocationRepository.save(allocation);
            
            log.info("Successfully cancelled allocation {} by user {}", allocationId, cancelledBy);

            return ResponseEntity.ok(
                new ApiResponse<>(true, "Allocation cancelled successfully", cancelledAllocation)
            );

        } catch (Exception e) {
            log.error("Error cancelling allocation {}: {}", allocationId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                new ApiResponse<>(false, "Error cancelling allocation: " + e.getMessage(), null)
            );
        }
    }

    @Override
    public ResponseEntity<ApiResponse> getAllocationsByResource(Long resourceId) {
        try {
            List<ResourceAllocation> allocations = allocationRepository.findByResource_ResourceId(resourceId);
            
            return ResponseEntity.ok(
                new ApiResponse<>(true, "Allocations retrieved successfully", allocations)
            );

        } catch (Exception e) {
            log.error("Error retrieving allocations for resource {}: {}", resourceId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                new ApiResponse<>(false, "Error retrieving allocations: " + e.getMessage(), null)
            );
        }
    }

    @Override
    public ResponseEntity<ApiResponse> getAllocationsByDemand(UUID demandId) {
        try {
            List<ResourceAllocation> allocations = allocationRepository.findByDemand_DemandId(demandId);
            
            return ResponseEntity.ok(
                new ApiResponse<>(true, "Allocations retrieved successfully", allocations)
            );

        } catch (Exception e) {
            log.error("Error retrieving allocations for demand {}: {}", demandId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                new ApiResponse<>(false, "Error retrieving allocations: " + e.getMessage(), null)
            );
        }
    }

    @Override
    public ResponseEntity<ApiResponse> getAllocationsByProject(Long projectId) {
        try {
            List<ResourceAllocation> allocations = allocationRepository.findByProject_PmsProjectId(projectId);
            
            return ResponseEntity.ok(
                new ApiResponse<>(true, "Allocations retrieved successfully", allocations)
            );

        } catch (Exception e) {
            log.error("Error retrieving allocations for project {}: {}", projectId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                new ApiResponse<>(false, "Error retrieving allocations: " + e.getMessage(), null)
            );
        }
    }
}
