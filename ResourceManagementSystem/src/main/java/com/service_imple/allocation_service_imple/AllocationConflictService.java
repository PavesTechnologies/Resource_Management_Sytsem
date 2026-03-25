package com.service_imple.allocation_service_imple;

import com.dto.allocation_dto.AllocationConflictDTO;
import com.dto.allocation_dto.AllocationRequestDTO;
import com.dto.allocation_dto.ConflictDetectionResult;
import com.dto.allocation_dto.ConflictResolutionDTO;
import com.dto.centralised_dto.ApiResponse;
import com.entity.allocation_entities.AllocationConflict;
import com.entity.allocation_entities.ResourceAllocation;
import com.entity.client_entities.Client;
import com.entity_enums.allocation_enums.AllocationStatus;
import com.entity_enums.centralised_enums.PriorityLevel;
import com.entity_enums.client_enums.ClientTier;
import com.entity_enums.demand_enums.DemandCommitment;
import com.repo.allocation_repo.AllocationConflictRepository;
import com.repo.allocation_repo.AllocationRepository;
import com.repo.demand_repo.DemandRepository;
import com.repo.project_repo.ProjectRepository;
import com.repo.resource_repo.ResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for detecting and resolving allocation conflicts
 */
@Service
@RequiredArgsConstructor
public class AllocationConflictService {

    private final AllocationConflictRepository conflictRepository;
    private final AllocationRepository allocationRepository;
    private final DemandRepository demandRepository;
    private final ProjectRepository projectRepository;
    private final ResourceRepository resourceRepository;

    /**
     * Detects priority conflicts for a new allocation request
     */
    @Transactional(readOnly = true)
    public ConflictDetectionResult detectPriorityConflicts(AllocationRequestDTO allocationRequest, Long resourceId) {
        try {
            // Fetch existing allocations for the specified resource
            List<ResourceAllocation> existingAllocations = allocationRepository.findByResource_ResourceId(resourceId);

            // Filter out ended and cancelled allocations
            List<ResourceAllocation> activeAllocations = existingAllocations.stream()
                    .filter(alloc -> alloc.getAllocationStatus() != AllocationStatus.ENDED &&
                                       alloc.getAllocationStatus() != AllocationStatus.CANCELLED)
                    .toList();

            // Check for priority conflicts
            for (ResourceAllocation existingAlloc : activeAllocations) {
                if (hasDateOverlap(allocationRequest, existingAlloc)) {
                    PriorityLevel newClientPriority = getClientPriority(allocationRequest);
                    PriorityLevel existingClientPriority = getClientPriority(existingAlloc);
                    
                    AllocationStatus newStatus = allocationRequest.getAllocationStatus();
                    AllocationStatus existingStatus = existingAlloc.getAllocationStatus();
                    
                    ClientTier newTier = mapPriorityLevelToClientTier(newClientPriority);
                    ClientTier existingTier = mapPriorityLevelToClientTier(existingClientPriority);
                    DemandCommitment newCommitment = mapAllocationStatusToDemandCommitment(newStatus);
                    DemandCommitment existingCommitment = mapAllocationStatusToDemandCommitment(existingStatus);
                    
                    if (isPriorityMismatch(existingTier, existingCommitment, newTier, newCommitment)) {
                        ConflictDetectionResult.PriorityConflictDetail conflict = createConflictDetail(
                            allocationRequest, existingAlloc, resourceId, newClientPriority, existingClientPriority);
                        
                        return ConflictDetectionResult.builder()
                                .hasConflicts(true)
                                .conflictType("PRIORITY_CONFLICT")
                                .severity("HIGH")
                                .conflicts(List.of(conflict))
                                .build();
                    }
                }
            }

            // No conflicts found
            return ConflictDetectionResult.builder()
                    .hasConflicts(false)
                    .build();

        } catch (Exception e) {
            return ConflictDetectionResult.builder()
                    .hasConflicts(true)
                    .conflictType("DETECTION_ERROR")
                    .severity("HIGH")
                    .message("Error detecting conflicts: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Optimized priority conflict detection using preloaded allocations
     */
    @Transactional(readOnly = true)
    public ConflictDetectionResult detectPriorityConflictsOptimized(
            AllocationRequestDTO allocationRequest, 
            Long resourceId, 
            List<ResourceAllocation> existingAllocations) {
        
        try {
            List<ConflictDetectionResult.PriorityConflictDetail> conflicts = new ArrayList<>();
            
            // Get priority of new allocation
            PriorityLevel newPriority = getClientPriority(allocationRequest);
            if (newPriority == null) {
                newPriority = PriorityLevel.MEDIUM; // Default priority
            }
            
            // Check against existing allocations
            for (ResourceAllocation existing : existingAllocations) {
                PriorityLevel existingPriority = getPriorityForAllocation(existing);
                
                // Check if there's a priority conflict
                if (existingPriority != null && 
                    newPriority.compareTo(existingPriority) > 0) {
                    
                    ConflictDetectionResult.PriorityConflictDetail conflict = 
                        ConflictDetectionResult.PriorityConflictDetail.builder()
                            .resourceId(resourceId)
                            .existingAllocationId(existing.getAllocationId())
                            .existingPriority(existingPriority)
                            .requestedPriority(newPriority)
                            .conflictReason("Higher priority allocation requested")
                            .build();
                    
                    conflicts.add(conflict);
                }
            }
            
            boolean hasConflicts = !conflicts.isEmpty();
            String message = hasConflicts ? 
                "Found " + conflicts.size() + " priority conflict(s)" : 
                "No priority conflicts detected";
            
            return ConflictDetectionResult.builder()
                .hasConflicts(hasConflicts)
                .message(message)
                .conflicts(conflicts)
                .build();
                
        } catch (Exception e) {
            return ConflictDetectionResult.builder()
                .hasConflicts(true)
                .message("Error detecting conflicts: " + e.getMessage())
                .build();
        }
    }

    /**
     * Gets pending conflicts for a resource
     */
    public List<AllocationConflictDTO> getPendingConflictsForResource(Long resourceId) {
        List<AllocationConflict> conflicts = conflictRepository.findByResource_ResourceIdAndResolutionStatus(resourceId, "PENDING");
        return conflicts.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Gets all pending conflicts
     */
    public List<AllocationConflictDTO> getAllPendingConflicts() {
        List<AllocationConflict> conflicts = conflictRepository.findAllPendingConflicts();
        return conflicts.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Detects allocation conflicts for a resource
     */
    public List<AllocationConflictDTO> detectAllocationConflicts(Long resourceId) {
        List<ResourceAllocation> allocations = allocationRepository.findByResource_ResourceId(resourceId);
        List<AllocationConflict> conflicts = new ArrayList<>();

        // Check all pairs of allocations for priority mismatches
        for (int i = 0; i < allocations.size(); i++) {
            for (int j = i + 1; j < allocations.size(); j++) {
                ResourceAllocation alloc1 = allocations.get(i);
                ResourceAllocation alloc2 = allocations.get(j);

                if (hasDateOverlap(alloc1, alloc2)) {
                    AllocationConflict conflict = checkPriorityMismatch(alloc1, alloc2);
                    if (conflict != null) {
                        conflicts.add(conflict);
                    }
                }
            }
        }

        // Save detected conflicts
        List<AllocationConflict> savedConflicts = conflictRepository.saveAll(conflicts);

        // Convert to DTOs and trigger alerts
        return savedConflicts.stream()
                .map(this::convertToDTO)
                .peek(this::triggerConflictAlert)
                .collect(Collectors.toList());
    }

    /**
     * Resolves a conflict with the specified action
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> resolveAllocationConflict(UUID conflictId, ConflictResolutionDTO resolution) {
        try {
            AllocationConflict conflict = conflictRepository.findById(conflictId)
                    .orElseThrow(() -> new RuntimeException("Conflict not found: " + conflictId));

            // Apply resolution action
            applyResolutionAction(conflict, resolution.getResolutionAction());

            // Update conflict status
            conflict.setResolutionStatus("RESOLVED");
            conflict.setResolutionAction(resolution.getResolutionAction());
            conflict.setResolvedBy(resolution.getResolvedBy());
            conflict.setResolvedAt(LocalDateTime.now());
            conflict.setResolutionNotes(resolution.getResolutionNotes());

            conflictRepository.save(conflict);

            return ResponseEntity.ok(new ApiResponse<>(true, "Conflict resolved successfully", null));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Error resolving conflict: " + e.getMessage(), null));
        }
    }

    // Helper methods
    private boolean hasDateOverlap(AllocationRequestDTO newAllocation, ResourceAllocation existingAllocation) {
        LocalDate newStart = newAllocation.getAllocationStartDate();
        LocalDate newEnd = newAllocation.getAllocationEndDate();
        LocalDate existingStart = existingAllocation.getAllocationStartDate();
        LocalDate existingEnd = existingAllocation.getAllocationEndDate();

        return overlaps(newStart, newEnd, existingStart, existingEnd);
    }

    private boolean hasDateOverlap(ResourceAllocation alloc1, ResourceAllocation alloc2) {
        return overlaps(
                alloc1.getAllocationStartDate(),
                alloc1.getAllocationEndDate(),
                alloc2.getAllocationStartDate(),
                alloc2.getAllocationEndDate()
        );
    }

    private boolean overlaps(LocalDate start1, LocalDate end1, LocalDate start2, LocalDate end2) {
        return !end1.isBefore(start2) && !start1.isAfter(end2);
    }

    private PriorityLevel getClientPriority(AllocationRequestDTO allocation) {
        if(allocation.getDemandId() != null) {
            var demand = demandRepository.findById(allocation.getDemandId())
                    .orElseThrow(() -> new RuntimeException("Demand not found"));
            return demand.getProject().getClient().getPriorityLevel();
        }

        if(allocation.getProjectId() != null) {
            var project = projectRepository.findById(allocation.getProjectId())
                    .orElseThrow(() -> new RuntimeException("Project not found"));
            return project.getClient().getPriorityLevel();
        }

        return PriorityLevel.LOW;
    }

    private PriorityLevel getClientPriority(ResourceAllocation allocation) {
        try {
            Client client = getClientFromAllocation(allocation);
            return client.getPriorityLevel();
        } catch (Exception e) {
            return PriorityLevel.LOW;
        }
    }

    private Client getClientFromAllocation(ResourceAllocation allocation) {
        if (allocation.getDemand() != null) {
            return allocation.getDemand().getProject().getClient();
        } else if (allocation.getProject() != null) {
            return allocation.getProject().getClient();
        }
        throw new RuntimeException("No client found for allocation: " + allocation.getAllocationId());
    }

    private PriorityLevel getPriorityForAllocation(ResourceAllocation allocation) {
        try {
            if (allocation.getDemand() != null) {
                DemandCommitment commitment = allocation.getDemand().getDemandCommitment();
                if (commitment == DemandCommitment.CONFIRMED) {
                    return PriorityLevel.HIGH;
                } else if (commitment == DemandCommitment.SOFT) {
                    return PriorityLevel.MEDIUM;
                }
            }
        } catch (Exception e) {
            // Error handling without logs
        }
        return PriorityLevel.LOW;
    }

    private ClientTier mapPriorityLevelToClientTier(PriorityLevel priorityLevel) {
        switch (priorityLevel) {
            case CRITICAL: return ClientTier.TIER_1_PLATINUM;
            case HIGH: return ClientTier.TIER_2_GOLD;
            case MEDIUM: return ClientTier.TIER_3_SILVER;
            case LOW: return ClientTier.TIER_4_BRONZE;
            default: return ClientTier.TIER_4_BRONZE;
        }
    }

    private DemandCommitment mapAllocationStatusToDemandCommitment(AllocationStatus status) {
        if (status == null) {
            return DemandCommitment.SOFT;
        }
        switch (status) {
            case ACTIVE:
                return DemandCommitment.CONFIRMED;
            case PLANNED:
                return DemandCommitment.SOFT;
            default:
                return DemandCommitment.CONFIRMED;
        }
    }

    private boolean isPriorityMismatch(ClientTier lowerTier, DemandCommitment lowerCommitment,
                                       ClientTier higherTier, DemandCommitment higherCommitment) {
        return lowerTier.isLowerPriorityThan(higherTier)
                && lowerCommitment == DemandCommitment.CONFIRMED
                && higherCommitment == DemandCommitment.SOFT;
    }

    private ConflictDetectionResult.PriorityConflictDetail createConflictDetail(
            AllocationRequestDTO allocationRequest, 
            ResourceAllocation existingAlloc, 
            Long resourceId,
            PriorityLevel newClientPriority, 
            PriorityLevel existingClientPriority) {
        
        return ConflictDetectionResult.PriorityConflictDetail.builder()
            .conflictId(UUID.randomUUID())
            .resourceId(resourceId)
            .resourceName(getResourceName(resourceId))
            .existingAllocationId(existingAlloc.getAllocationId())
            .conflictType("PRIORITY_CONFLICT")
            .severity("HIGH")
            .message(String.format(
                    "Priority conflict detected: Higher priority client (%s - Tier %s) requesting PLANNED allocation " +
                    "while lower priority client (%s - Tier %s) has ACTIVE allocation for resource %s",
                    getClientName(allocationRequest), newClientPriority.getDisplayName(),
                    getClientName(existingAlloc), existingClientPriority.getDisplayName(),
                    getResourceName(resourceId))
            )
            .existingClientName(getClientName(existingAlloc))
            .existingClientTier(existingClientPriority.getDisplayName())
            .existingAllocationType(existingAlloc.getAllocationStatus().name())
            .newClientName(getClientName(allocationRequest))
            .newClientTier(newClientPriority.getDisplayName())
            .newAllocationType(allocationRequest.getAllocationStatus().name())
            .recommendedActions(getRecommendedActions())
            .build();
    }

    private String getClientName(AllocationRequestDTO allocation) {
        return "Client Name"; // Placeholder - implement based on DTO structure
    }

    private String getClientName(ResourceAllocation allocation) {
        try {
            return getClientFromAllocation(allocation).getClientName();
        } catch (Exception e) {
            return "Unknown Client";
        }
    }

    private String getResourceName(Long resourceId) {
        try {
            var resourceOptional = resourceRepository.findById(resourceId);
            return resourceOptional.map(resource -> resource.getFullName()).orElse("Resource " + resourceId);
        } catch (Exception e) {
            return "Resource " + resourceId;
        }
    }

    private List<String> getRecommendedActions() {
        return List.of(
                "Upgrade higher-priority allocation from PLANNED → ACTIVE",
                "Downgrade or cancel lower-priority ACTIVE allocation",
                "Override and keep existing allocation"
        );
    }

    private AllocationConflict checkPriorityMismatch(ResourceAllocation alloc1, ResourceAllocation alloc2) {
        ClientTier tier1 = getClientTier(alloc1);
        ClientTier tier2 = getClientTier(alloc2);
        DemandCommitment commitment1 = getDemandCommitment(alloc1);
        DemandCommitment commitment2 = getDemandCommitment(alloc2);

        // Check for priority mismatch
        if (isPriorityMismatch(tier1, commitment1, tier2, commitment2)) {
            return createConflictEntity(alloc1, alloc2, tier1, tier2, commitment1, commitment2);
        } else if (isPriorityMismatch(tier2, commitment2, tier1, commitment1)) {
            return createConflictEntity(alloc2, alloc1, tier2, tier1, commitment2, commitment1);
        }

        return null;
    }

    private ClientTier getClientTier(ResourceAllocation allocation) {
        Client client = getClientFromAllocation(allocation);
        return mapPriorityLevelToClientTier(client.getPriorityLevel());
    }

    private DemandCommitment getDemandCommitment(ResourceAllocation allocation) {
        if (allocation.getDemand() != null) {
            return allocation.getDemand().getDemandCommitment();
        }
        return DemandCommitment.CONFIRMED;
    }

    private AllocationConflict createConflictEntity(ResourceAllocation lowerPriorityAlloc, ResourceAllocation higherPriorityAlloc,
                                            ClientTier lowerTier, ClientTier higherTier,
                                            DemandCommitment lowerCommitment, DemandCommitment higherCommitment) {

        String severity = determineConflictSeverity(lowerTier, higherTier);
        String recommendation = generateRecommendation(lowerTier, higherTier, lowerCommitment, higherCommitment);

        return AllocationConflict.builder()
                .resource(lowerPriorityAlloc.getResource())
                .lowerPriorityAllocationId(lowerPriorityAlloc.getAllocationId())
                .higherPriorityAllocationId(higherPriorityAlloc.getAllocationId())
                .lowerPriorityClientName(getClientName(lowerPriorityAlloc))
                .higherPriorityClientName(getClientName(higherPriorityAlloc))
                .lowerPriorityClientTier(lowerTier.getDisplayName())
                .higherPriorityClientTier(higherTier.getDisplayName())
                .lowerPriorityAllocationType(lowerCommitment.name())
                .higherPriorityAllocationType(higherCommitment.name())
                .conflictType("PRIORITY_MISMATCH")
                .conflictSeverity(severity)
                .recommendation(recommendation)
                .resolutionStatus("PENDING")
                .build();
    }

    private String determineConflictSeverity(ClientTier lowerTier, ClientTier higherTier) {
        int tierDifference = Math.abs(lowerTier.getLevel() - higherTier.getLevel());
        if (tierDifference >= 3) {
            return "HIGH";
        } else if (tierDifference >= 2) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }

    private String generateRecommendation(ClientTier lowerTier, ClientTier higherTier,
                                       DemandCommitment lowerCommitment, DemandCommitment higherCommitment) {
        return String.format("Consider upgrading the %s client's %s allocation or adjusting the %s client's %s allocation.",
                higherTier.getDisplayName(), higherCommitment.name(),
                lowerTier.getDisplayName(), lowerCommitment.name());
    }

    private AllocationConflictDTO convertToDTO(AllocationConflict conflict) {
        // Simplified DTO conversion - implement full conversion as needed
        return AllocationConflictDTO.builder()
                .conflictId(conflict.getConflictId())
                .resourceId(conflict.getResource().getResourceId())
                .resourceName(conflict.getResource().getFullName())
                .conflictType(conflict.getConflictType())
                .conflictSeverity(conflict.getConflictSeverity())
                .recommendation(conflict.getRecommendation())
                .resolutionStatus(conflict.getResolutionStatus())
                .build();
    }

    private void triggerConflictAlert(AllocationConflictDTO conflict) {
        // Conflict alert without logs
    }

    private void applyResolutionAction(AllocationConflict conflict, String action) {
        switch (action) {
            case "UPGRADE":
                upgradeAllocation(conflict.getHigherPriorityAllocationId());
                break;
            case "DISPLACE":
                displaceAllocation(conflict.getLowerPriorityAllocationId());
                break;
            case "KEEP_CURRENT":
                // Keeping current allocation
                break;
            default:
                throw new IllegalArgumentException("Unknown resolution action: " + action);
        }
    }

    private void upgradeAllocation(UUID allocationId) {
        ResourceAllocation allocation = allocationRepository.findById(allocationId)
                .orElseThrow(() -> new RuntimeException("Allocation not found: " + allocationId));

        if (allocation.getDemand() != null) {
            allocation.getDemand().setDemandCommitment(DemandCommitment.CONFIRMED);
        }

        allocationRepository.save(allocation);
    }

    private void displaceAllocation(UUID allocationId) {
        ResourceAllocation allocation = allocationRepository.findById(allocationId)
                .orElseThrow(() -> new RuntimeException("Allocation not found: " + allocationId));

        allocation.setAllocationStatus(AllocationStatus.CANCELLED);
        allocationRepository.save(allocation);
    }
}
