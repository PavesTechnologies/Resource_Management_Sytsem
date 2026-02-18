package com.service_imple.allocation_service_imple;

import com.dto.allocation_dto.AllocationRequestDTO;
import com.dto.ApiResponse;
import com.entity.allocation_entities.ResourceAllocation;
import com.entity.availability_entities.ResourceAvailabilityLedger;
import com.entity.skill_entities.ResourceSkill;
import com.entity.skill_entities.ResourceCertificate;
import com.entity.skill_entities.Skill;
import com.entity.skill_entities.Certificate;
import com.entity_enums.allocation_enums.AllocationStatus;
import com.global_exception_handler.ProjectExceptionHandler;
import com.repo.allocation_repo.AllocationRepository;
import com.repo.resource_repo.ResourceRepository;
import com.repo.DemandRepository;
import com.repo.project_repo.ProjectRepository;
import com.repo.availability_repo.ResourceAvailabilityLedgerRepository;
import com.repo.skill_repo.ResourceSkillRepository;
import com.repo.skill_repo.ResourceCertificateRepository;
import com.service_interface.allocation_service_interface.AllocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AllocationServiceImple implements AllocationService {

    private final AllocationRepository allocationRepository;
    private final ResourceRepository resourceRepository;
    private final DemandRepository demandRepository;
    private final ProjectRepository projectRepository;
    private final ResourceAvailabilityLedgerRepository ledgerRepository;
    private final ResourceSkillRepository resourceSkillRepository;
    private final ResourceCertificateRepository resourceCertificateRepository;

    @Override
    public ResponseEntity<ApiResponse<?>> assignAllocation(AllocationRequestDTO allocationRequest) {
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

            // 🔹 Validate Skills and Certifications if demand exists
            if (allocation.getDemand() != null) {
                validateAllocationRequirements(allocationRequest.getResourceId(), allocation.getDemand());
            }

            ResourceAllocation savedAllocation = allocationRepository.save(allocation);
            
            // Update availability ledger for the allocation period
            updateAvailabilityLedgerForAllocation(savedAllocation);
            
            
            return ResponseEntity.ok(
                new ApiResponse<>(true, "Allocation created successfully", savedAllocation)
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
                return ResponseEntity.ok(
                    new ApiResponse<>(true, "Allocation found", allocation.get())
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
            
            // Update fields
            allocation.setAllocationStartDate(allocationRequest.getAllocationStartDate());
            allocation.setAllocationEndDate(allocationRequest.getAllocationEndDate());
            allocation.setAllocationPercentage(allocationRequest.getAllocationPercentage());
            allocation.setAllocationStatus(allocationRequest.getAllocationStatus());

            ResourceAllocation updatedAllocation = allocationRepository.save(allocation);
            
            // Update availability ledger for the modified allocation period
            updateAvailabilityLedgerForAllocation(updatedAllocation);
            
            
            return ResponseEntity.ok(
                new ApiResponse<>(true, "Allocation updated successfully", updatedAllocation)
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
            
            
            return ResponseEntity.ok(
                new ApiResponse<>(true, "Allocation cancelled successfully", cancelledAllocation)
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
            
            return ResponseEntity.ok(
                new ApiResponse<>(true, "Allocations retrieved successfully", allocations)
            );

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                new ApiResponse<>(false, "Error retrieving allocations: " + e.getMessage(), null)
            );
        }
    }

    @Override
    public ResponseEntity<ApiResponse<?>> getAllocationsByDemand(UUID demandId) {
        try {
            List<ResourceAllocation> allocations = allocationRepository.findByDemand_DemandId(demandId);
            
            return ResponseEntity.ok(
                new ApiResponse<>(true, "Allocations retrieved successfully", allocations)
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
            
            return ResponseEntity.ok(
                new ApiResponse<>(true, "Allocations retrieved successfully", allocations)
            );

        } catch (Exception e) {
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
            
            // Find existing ledger entry using date range (handles monthly ledgers)
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
                    }
    }

    /**
     * Validates that the resource has all required skills and certificates for the demand
     */
    private void validateAllocationRequirements(Long resourceId, com.entity.demand_entities.Demand demand) {
        LocalDate currentDate = LocalDate.now();
        
        // 🔹 Validate Skills
        if (demand.getRequiredSkills() != null && !demand.getRequiredSkills().isEmpty()) {
            List<ResourceSkill> resourceSkills = resourceSkillRepository
                    .findByResourceIdAndActiveFlagTrue(resourceId);
            
            Set<UUID> resourceSkillIds = resourceSkills.stream()
                    .filter(rs -> rs.getExpiryDate() == null || rs.getExpiryDate().isAfter(currentDate))
                    .map(ResourceSkill::getSkillId)
                    .collect(Collectors.toSet());
            
            Set<UUID> requiredSkillIds = demand.getRequiredSkills().stream()
                    .map(Skill::getId)
                    .collect(Collectors.toSet());
            
            requiredSkillIds.removeAll(resourceSkillIds);
            
            if (!requiredSkillIds.isEmpty()) {
                List<String> missingSkillNames = demand.getRequiredSkills().stream()
                        .filter(skill -> requiredSkillIds.contains(skill.getId()))
                        .map(Skill::getName)
                        .collect(Collectors.toList());
                
                throw new ProjectExceptionHandler(
                        HttpStatus.BAD_REQUEST,
                        "MISSING_REQUIRED_SKILLS",
                        "Missing required skills: " + String.join(", ", missingSkillNames)
                );
            }
        }
        
        // 🔹 Validate Certificates
        if (demand.getRequiredCertificates() != null && !demand.getRequiredCertificates().isEmpty()) {
            List<ResourceCertificate> resourceCertificates = resourceCertificateRepository
                    .findActiveCertificatesForResource(resourceId, currentDate);
            
            Set<UUID> resourceCertificateIds = resourceCertificates.stream()
                    .map(ResourceCertificate::getCertificateId)
                    .collect(Collectors.toSet());
            
            Set<UUID> requiredCertificateIds = demand.getRequiredCertificates().stream()
                    .map(Certificate::getCertificateId)
                    .collect(Collectors.toSet());
            
            requiredCertificateIds.removeAll(resourceCertificateIds);
            
            if (!requiredCertificateIds.isEmpty()) {
                List<String> missingCertificateNames = demand.getRequiredCertificates().stream()
                        .filter(cert -> requiredCertificateIds.contains(cert.getCertificateId()))
                        .map(cert -> cert.getProviderName() != null ? cert.getProviderName() : cert.getCertificateId().toString())
                        .collect(Collectors.toList());
                
                throw new ProjectExceptionHandler(
                        HttpStatus.BAD_REQUEST,
                        "MISSING_REQUIRED_CERTIFICATIONS",
                        "Missing required certifications: " + String.join(", ", missingCertificateNames)
                );
            }
        }
    }
}
