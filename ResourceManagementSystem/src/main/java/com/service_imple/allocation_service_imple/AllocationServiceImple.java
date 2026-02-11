package com.service_imple.allocation_service_imple;

import com.dto.AllocationRequestDTO;
import com.dto.ApiResponse;
import com.entity.allocation_entities.ResourceAllocation;
import com.entity.availability_entities.ResourceAvailabilityLedger;
import com.entity_enums.allocation_enums.AllocationStatus;
import com.repo.allocation_repo.AllocationRepository;
import com.repo.resource_repo.ResourceRepository;
import com.repo.DemandRepository;
import com.repo.project_repo.ProjectRepository;
import com.repo.availability_repo.ResourceAvailabilityLedgerRepository;
import com.service_interface.allocation_service_interface.AllocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalDate;
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
    private final ResourceAvailabilityLedgerRepository ledgerRepository;

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
            
            // Update availability ledger for the allocation period
            updateAvailabilityLedgerForAllocation(savedAllocation);
            
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
            
            // Update availability ledger for the modified allocation period
            updateAvailabilityLedgerForAllocation(updatedAllocation);
            
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
            
            // Update availability ledger to reflect cancellation
            updateAvailabilityLedgerForAllocation(cancelledAllocation);
            
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

    /**
     * Updates the ResourceAvailabilityLedger for the given allocation.
     * This method recalculates the total allocation percentage for each day
     * in the allocation period and updates the ledger accordingly.
     */
    private void updateAvailabilityLedgerForAllocation(ResourceAllocation allocation) {
        try {
            Long resourceId = allocation.getResource().getResourceId();
            LocalDate startDate = allocation.getAllocationStartDate();
            LocalDate endDate = allocation.getAllocationEndDate();
            
            log.debug("Updating availability ledger for resource {} from {} to {}", resourceId, startDate, endDate);
            
            // For each day in the allocation period, update the ledger
            LocalDate currentDate = startDate;
            while (!currentDate.isAfter(endDate)) {
                updateLedgerForDate(resourceId, currentDate);
                currentDate = currentDate.plusDays(1);
            }
            
            log.info("Successfully updated availability ledger for allocation {} affecting {} days", 
                    allocation.getAllocationId(), java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1);
                    
        } catch (Exception e) {
            log.error("Error updating availability ledger for allocation {}: {}", 
                    allocation.getAllocationId(), e.getMessage(), e);
            // Don't throw here to avoid failing the main allocation operation
        }
    }

    /**
     * Updates the ledger for a specific resource and date by calculating
     * the total allocation percentage from all active allocations.
     */
    private void updateLedgerForDate(Long resourceId, LocalDate date) {
        try {
            // Calculate total allocation percentage for this resource on this date
            List<ResourceAllocation> activeAllocations = allocationRepository
                .findActiveAllocationsForResourceOnDate(resourceId, date);
            
            int totalAllocation = activeAllocations.stream()
                .mapToInt(ResourceAllocation::getAllocationPercentage)
                .sum();
            
            // Find existing ledger entry or create new one
            Optional<ResourceAvailabilityLedger> existingLedger = 
                ledgerRepository.findByResourceIdAndPeriodStart(resourceId, date);
            
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
            log.error("Error updating ledger for resource {} on date {}: {}", resourceId, date, e.getMessage());
        }
    }
}
