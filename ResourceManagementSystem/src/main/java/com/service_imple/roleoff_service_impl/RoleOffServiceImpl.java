package com.service_imple.roleoff_service_impl;

import com.dto.centralised_dto.ApiResponse;
import com.dto.centralised_dto.UserDTO;
import com.dto.allocation_dto.CloseAllocationDTO;
import com.dto.allocation_dto.RoleOffRequestDTO;
import com.dto.roleoff_dto.ResourcesDTO;
import com.dto.roleoff_dto.BulkRoleOffRequestDTO;
import com.dto.demand_dto.CreateDemandDTO;
import com.dto.resource_dto.ResourceRemovalDTO;
import com.entity.allocation_entities.ResourceAllocation;
import com.entity.roleoff_entities.RoleOffEvent;
import com.entity.project_entities.Project;
import com.entity.resource_entities.Resource;
import com.entity.skill_entities.DeliveryRoleExpectation;
import com.entity_enums.allocation_enums.AllocationStatus;
import com.entity_enums.roleoff_enums.RoleOffStatus;
import com.entity_enums.roleoff_enums.RoleOffType;
import com.entity_enums.demand_enums.DemandCommitment;
import com.entity_enums.demand_enums.DemandStatus;
import com.entity_enums.demand_enums.DemandType;
import com.entity_enums.demand_enums.ReplacementStatus;
import com.entity_enums.resource_enums.EmploymentStatus;
import com.entity_enums.roleoff_enums.RoleOffReason;
import com.global_exception_handler.ProjectExceptionHandler;
import com.repo.allocation_repo.AllocationRepository;
import com.repo.roleoff_repo.RoleOffEventRepository;
import com.repo.project_repo.ProjectRepository;
import com.repo.resource_repo.ResourceRepository;
import com.repo.skill_repo.DeliveryRoleExpectationRepository;
import com.repo.skill_repo.ResourceSkillRepository;
import com.repo.demand_repo.DemandRepository;
import com.repo.skill_repo.ResourceSubSkillRepository;
import com.service_interface.allocation_service_interface.AllocationService;
import com.service_interface.demand_service_interface.DemandService;
import com.service_interface.roleoff_service_interface.RoleOffService;
import com.service_imple.bench_service_impl.BenchService;
import com.service_imple.allocation_service_imple.AvailabilityLedgerAsyncServiceRefactored;
import com.service_imple.skill_service_impl.ResourceSkillUsageService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import java.time.LocalDate;
import java.util.*;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleOffServiceImpl implements RoleOffService {

    private final RoleOffEventRepository roleOffRepo;
    private final ResourceRepository resourceRepo;
    private final ProjectRepository projectRepo;
    private final AllocationRepository allocationRepository;
    private final DemandRepository demandRepo;
    private final ResourceSkillRepository resourceSkillRepo;
    private final ResourceSubSkillRepository resourceSubSkillRepo;
    private final DeliveryRoleExpectationRepository deliveryRoleExpectationRepo;
    private final AllocationService allocationService;
    private final DemandService demandService;
    private final BenchService benchService;
    private final AvailabilityLedgerAsyncServiceRefactored availabilityLedgerAsyncService;
    private final ResourceSkillUsageService resourceSkillUsageService;

    // Role-off reason descriptions for governance purposes
    private static final Map<RoleOffReason, String> REASON_DESCRIPTIONS = Map.of(
        RoleOffReason.PERFORMANCE, "Performance-related role-off due to underperformance or misconduct",
        RoleOffReason.CLIENT_REQUEST, "Client requested resource removal from project",
        RoleOffReason.PROJECT_END, "Natural project completion leading to role-off",
        RoleOffReason.ATTRITION, "Employee resignation or voluntary exit"
    );

// ========== MAIN ROLE-OFF METHODS ==========

@Override
public ResponseEntity<?> roleOffByRM(RoleOffRequestDTO roleOff, UserDTO userDTO) {
    try {
        // Validate role-off reason
        validateRoleOffReason(roleOff.getRoleOffReason());

        // Get allocation details
        ResourceAllocation allocation = allocationRepository.findById(roleOff.getAllocationId())
                .orElseThrow(() -> new ProjectExceptionHandler(
                        HttpStatus.NOT_FOUND,
                        "ALLOCATION_NOT_FOUND",
                        "Allocation not found with ID: " + roleOff.getAllocationId()));

        // Get resource and project details
        Resource resource = allocation.getResource();
        Project project = allocation.getProject();

        // Validate ownership
        if (!project.getProjectManagerId().equals(userDTO.getId())) {
            throw new ProjectExceptionHandler(
                    HttpStatus.FORBIDDEN,
                    "PROJECT_ACCESS_DENIED",
                    "You can only create role-offs for your own projects");
        }

        // Generate impact preview if not confirmed
        if (roleOff.getConfirmed() == null || !roleOff.getConfirmed()) {
            return generateImpactPreview(roleOff, userDTO.getId(), resource, project, allocation);
        }

        // Process the role-off
        processRoleOff(roleOff, userDTO.getId(), resource, project, allocation);

        return ResponseEntity.ok(new ApiResponse<>(true, "Role-off request created successfully", null));

    } catch (ProjectExceptionHandler e) {
        throw e;
    } catch (Exception e) {
        throw new ProjectExceptionHandler(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "ROLE_OFF_CREATION_FAILED",
                "Failed to create role-off request: " + e.getMessage());
    }
}

@Override
public void manualReplacement(UUID roleOffEventId, Long userId) {
    try {
        // Find the role-off event
        RoleOffEvent roleOffEvent = roleOffRepo.findById(roleOffEventId)
                .orElseThrow(() -> new ProjectExceptionHandler(
                        HttpStatus.NOT_FOUND,
                        "ROLE_OFF_EVENT_NOT_FOUND",
                        "Role-off event not found with ID: " + roleOffEventId));

        // Validate that role-off is in appropriate status for replacement
        if (roleOffEvent.getRoleOffStatus() != RoleOffStatus.APPROVED &&
            roleOffEvent.getRoleOffStatus() != RoleOffStatus.FULFILLED) {
            throw new ProjectExceptionHandler(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_ROLE_OFF_STATUS",
                    "Manual replacement can only be created for approved or fulfilled role-offs");
        }

        // Create replacement demand from the role-off allocation
        ResourceAllocation allocation = roleOffEvent.getAllocation();
        if (allocation != null) {
            demandService.createReplacementDemandFromAllocation(
                    allocation,
                    LocalDate.now(),
                    allocation.getAllocationEndDate(),
                    userId);
        }

        log.info("Manual replacement created for role-off event: {}", roleOffEventId);

    } catch (ProjectExceptionHandler e) {
        throw e;
    } catch (Exception e) {
        throw new ProjectExceptionHandler(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "MANUAL_REPLACEMENT_FAILED",
                "Failed to create manual replacement: " + e.getMessage());
    }
}

// ========== GET METHODS FOR ROLE-OFF DETAILS ==========

@Override
public List<RoleOffEvent> getAllRoleOffEvents() {
    return roleOffRepo.findAll();
}

@Override
public List<RoleOffEvent> getRoleOffEventsByProject(Long projectId) {
    return roleOffRepo.findByProject_PmsProjectId(projectId);
}

@Override
public List<RoleOffEvent> getRoleOffEventsByResource(Long resourceId) {
    return roleOffRepo.findByResource_ResourceId(resourceId);
}

@Override
public RoleOffEvent getRoleOffEventById(UUID id) {
    return roleOffRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("Role-off event not found with ID: " + id));
}

// ========== GOVERNANCE METHODS ==========

/**
 * Validates that the provided role-off reason is valid and not null
 */
public void validateRoleOffReason(RoleOffReason reason) {
    if (reason == null) {
        throw new ProjectExceptionHandler(
                HttpStatus.BAD_REQUEST,
                "ROLE_OFF_REASON_REQUIRED",
                "Role-off reason classification is mandatory. Must be one of: " + getValidReasonsString()
        );
    }

    // Additional validation can be added here based on business rules
    validateBusinessRules(reason);
}

/**
 * Gets all valid role-off reasons for dropdown selections
 */
public List<RoleOffReason> getValidRoleOffReasons() {
    return Arrays.asList(RoleOffReason.values());
}

/**
 * Gets description for a specific role-off reason
 */
public String getReasonDescription(RoleOffReason reason) {
    return REASON_DESCRIPTIONS.get(reason);
}

/**
 * Validates role-off event data for governance compliance
 */
public void validateRoleOffEvent(RoleOffEvent event) {
    if (event.getRoleOffReason() == null) {
        throw new ProjectExceptionHandler(
                HttpStatus.BAD_REQUEST,
                "ROLE_OFF_REASON_REQUIRED",
                "Role-off event must have a classified reason"
        );
    }

    // Additional governance validations can be added here
    validateBusinessRules(event.getRoleOffReasonEnum());
}

/**
 * Gets statistics for role-off reasons reporting
 */
public Map<RoleOffReason, Long> getRoleOffReasonStatistics() {
    return roleOffRepo.findAll().stream()
            .filter(event -> event.getRoleOffReason() != null)
            .collect(Collectors.groupingBy(
                    RoleOffEvent::getRoleOffReasonEnum,
                    Collectors.counting()
            ));
}

/**
 * Checks if a role-off reason is valid
 */
public boolean isValidRoleOffReason(RoleOffReason reason) {
    return reason != null && Arrays.asList(RoleOffReason.values()).contains(reason);
}

/**
 * Business rules validation for specific reasons
 */
private void validateBusinessRules(RoleOffReason reason) {
    switch (reason) {
        case PERFORMANCE:
            // Performance reasons might require additional documentation
            // This can be extended based on business requirements
            break;
        case CLIENT_REQUEST:
            // Client requests might need client approval references
            // This can be extended based on business requirements
            break;
        case PROJECT_END:
            // Project end might require project closure validation
            // This can be extended based on business requirements
            break;
        case ATTRITION:
            // Attrition might require HR process validation
            // This can be extended based on business requirements
            break;
    }
}

/**
 * Gets comma-separated string of valid reasons for error messages
 */
private String getValidReasonsString() {
    return Arrays.stream(RoleOffReason.values())
            .map(Enum::name)
            .collect(Collectors.joining(", "));
}

// ========== DELIVERY IMPACT VALIDATION ==========

/**
 * Batch calculate impact levels for multiple resources to avoid N+1 queries
 */
private Map<Long, String> calculateBatchImpactLevels(List<Long> resourceIds, Long projectId) {
    if (resourceIds.isEmpty()) {
        return new HashMap<>();
    }

    Map<Long, String> impactLevelsMap = new HashMap<>();

    try {
        // Batch fetch all required data
        Map<Long, Integer> totalUtilizations = new HashMap<>();
        Map<Long, List<Integer>> projectAllocationsMap = new HashMap<>();

        // Fetch utilization data for all resources in batch
        List<Object[]> totalUtilData = roleOffRepo.getTotalCurrentUtilizationBatch(resourceIds);
        for (Object[] row : totalUtilData) {
            Long resourceId = (Long) row[0];
            Integer utilization = (Integer) row[1];
            totalUtilizations.put(resourceId, utilization);
        }

        List<Object[]> projectAllocData = roleOffRepo.getCurrentUtilizationBatch(resourceIds);
        for (Object[] row : projectAllocData) {
            Long resourceId = (Long) row[0];
            Integer allocation = (Integer) row[1];
            projectAllocationsMap.computeIfAbsent(resourceId, k -> new ArrayList<>()).add(allocation);
        }

        // Fetch all resources in batch
        List<Resource> resources = resourceRepo.findAllById(resourceIds);
        Map<Long, Resource> resourcesMap = resources.stream()
                .collect(Collectors.toMap(Resource::getResourceId, r -> r));

        // Fetch project data once
        String projectPriority = roleOffRepo.getProjectPriorityLevel(projectId);
        long currentAllocations = roleOffRepo.countActiveAllocationsForProject(projectId);
        List<Integer> requiredPositions = roleOffRepo.getRequiredPositionsForProject(projectId);
        int totalRequired = requiredPositions.stream().mapToInt(Integer::intValue).sum();
        int gapAfterRoleOff = Math.max(0, totalRequired - (int)(currentAllocations - resourceIds.size()));

        // Calculate impact for each resource
        for (Long resourceId : resourceIds) {
            int impactScore = 0;

            // 1. UTILIZATION IMPACT (0-40 points)
            List<Integer> projectAllocations = projectAllocationsMap.getOrDefault(resourceId, List.of());
            int projectUtilization = projectAllocations.stream()
                    .filter(alloc -> alloc != null)
                    .findFirst()
                    .orElse(0);

            if (projectUtilization >= 80) impactScore += 40;
            else if (projectUtilization >= 50) impactScore += 25;
            else if (projectUtilization >= 20) impactScore += 10;

            // 2. PROJECT CRITICALITY (0-30 points)
            if ("CRITICAL".equals(projectPriority)) impactScore += 30;
            else if ("HIGH".equals(projectPriority)) impactScore += 20;
            else if ("NORMAL".equals(projectPriority)) impactScore += 5;

            if (gapAfterRoleOff > 2) impactScore += 15;
            else if (gapAfterRoleOff > 0) impactScore += 10;

            // 3. SKILL CRITICALITY (0-20 points)
            Resource resource = resourcesMap.get(resourceId);
            if (resource != null) {
                String primarySkill = resource.getPrimarySkillGroup();
                if ("TECHNICAL".equals(primarySkill) || "LEAD".equals(primarySkill)) impactScore += 15;
                else if ("SUPPORT".equals(primarySkill) || "ANALYST".equals(primarySkill)) impactScore += 10;
                else impactScore += 5;

                // Experience impact
                Long experience = resource.getExperiance();
                if (experience != null && experience >= 5) impactScore += 5;
                else if (experience != null && experience >= 2) impactScore += 3;
            }

            // 4. TIMELINE IMPACT (0-10 points) - simplified for batch
            impactScore += 5; // Default medium impact for timeline

            // Determine impact level
            String impactLevel;
            if (impactScore >= 70) impactLevel = "HIGH";
            else if (impactScore >= 40) impactLevel = "MEDIUM";
            else impactLevel = "LOW";

            impactLevelsMap.put(resourceId, impactLevel);
        }

    } catch (Exception e) {
        // Fallback to LOW impact for all resources if calculation fails
        for (Long resourceId : resourceIds) {
            impactLevelsMap.put(resourceId, "LOW");
        }
    }

    return impactLevelsMap;
}

/**
 * Calculate simple resource impact level (LOW/MEDIUM/HIGH)
 * Returns impact level based on utilization, project criticality, and skill factors
 */
public String calculateResourceImpactLevel(Long resourceId, Long projectId) {
    try {
        int impactScore = 0;

        // 1. UTILIZATION IMPACT (0-40 points)
        Integer totalUtilization = roleOffRepo.getTotalCurrentUtilization(resourceId);
        List<Integer> projectAllocations = roleOffRepo.getCurrentUtilization(resourceId);
        int projectUtilization = projectAllocations.stream()
                .filter(alloc -> alloc != null)
                .findFirst()
                .orElse(0);

        if (projectUtilization >= 80) impactScore += 40;      // High utilization
        else if (projectUtilization >= 50) impactScore += 25; // Medium utilization
        else if (projectUtilization >= 20) impactScore += 10; // Low utilization

        // 2. PROJECT CRITICALITY (0-30 points)
        String projectPriority = roleOffRepo.getProjectPriorityLevel(projectId);
        if ("CRITICAL".equals(projectPriority)) impactScore += 30;
        else if ("HIGH".equals(projectPriority)) impactScore += 20;
        else if ("NORMAL".equals(projectPriority)) impactScore += 5;

        // Check resource gap
        long currentAllocations = roleOffRepo.countActiveAllocationsForProject(projectId);
        List<Integer> requiredPositions = roleOffRepo.getRequiredPositionsForProject(projectId);
        int totalRequired = requiredPositions.stream().mapToInt(Integer::intValue).sum();
        int gapAfterRoleOff = Math.max(0, totalRequired - (int)(currentAllocations - 1));

        if (gapAfterRoleOff > 2) impactScore += 15;
        else if (gapAfterRoleOff > 0) impactScore += 10;

        // 3. SKILL CRITICALITY (0-20 points)
        Resource resource = resourceRepo.findById(resourceId)
                .orElseThrow(() -> new RuntimeException("Resource not found"));

        // Primary skill impact
        String primarySkill = resource.getPrimarySkillGroup();
        if ("TECHNICAL".equals(primarySkill) || "LEAD".equals(primarySkill)) impactScore += 15;
        else if ("SUPPORT".equals(primarySkill) || "ANALYST".equals(primarySkill)) impactScore += 10;
        else impactScore += 5;

        // Experience impact
        Long experience = resource.getExperiance();
        if (experience != null && experience >= 5) impactScore += 5;
        else if (experience != null && experience >= 2) impactScore += 3;

        // 4. TIMELINE IMPACT (0-10 points)
        Optional<ResourceAllocation> allocation = allocationRepository
                .findByProject_PmsProjectIdAndResource_ResourceIdAndAllocationStatus(
                        projectId, resourceId, AllocationStatus.ACTIVE);

        if (allocation.isPresent()) {
            long daysRemaining = java.time.temporal.ChronoUnit.DAYS.between(
                    java.time.LocalDate.now(), allocation.get().getAllocationEndDate());

            if (daysRemaining > 90) impactScore += 2;      // Long time remaining
            else if (daysRemaining > 30) impactScore += 5;  // Medium time remaining
            else if (daysRemaining > 0) impactScore += 10;  // Short time remaining - critical
        }

        // Determine impact level based on total score
        if (impactScore >= 70) return "HIGH";
        else if (impactScore >= 40) return "MEDIUM";
        else return "LOW";

    } catch (Exception e) {
        return "MEDIUM"; // Default fallback
    }
}

/**
 * Get impact score details (for debugging or detailed view)
 */
public Map<String, Object> getImpactScoreDetails(Long resourceId, Long projectId) {
    Map<String, Object> details = new HashMap<>();

    try {
        // Utilization details
        Integer totalUtilization = roleOffRepo.getTotalCurrentUtilization(resourceId);
        List<Integer> projectAllocations = roleOffRepo.getCurrentUtilization(resourceId);
        int projectUtilization = projectAllocations.stream()
                .filter(alloc -> alloc != null)
                .findFirst()
                .orElse(0);

        details.put("projectUtilization", projectUtilization);
        details.put("totalUtilization", totalUtilization);

        // Project details
        String projectPriority = roleOffRepo.getProjectPriorityLevel(projectId);
        long currentAllocations = roleOffRepo.countActiveAllocationsForProject(projectId);
        List<Integer> requiredPositions = roleOffRepo.getRequiredPositionsForProject(projectId);
        int totalRequired = requiredPositions.stream().mapToInt(Integer::intValue).sum();
        int gapAfterRoleOff = Math.max(0, totalRequired - (int)(currentAllocations - 1));

        details.put("projectPriority", projectPriority);
        details.put("resourceGap", gapAfterRoleOff);

        // Resource details
        Resource resource = resourceRepo.findById(resourceId).orElse(null);
        details.put("primarySkill", resource.getPrimarySkillGroup());
        details.put("experience", resource.getExperiance());

        // Timeline details
        Optional<ResourceAllocation> allocation = allocationRepository
                .findByProject_PmsProjectIdAndResource_ResourceIdAndAllocationStatus(
                        projectId, resourceId, AllocationStatus.ACTIVE);

        if (allocation.isPresent()) {
            long daysRemaining = java.time.temporal.ChronoUnit.DAYS.between(
                    java.time.LocalDate.now(), allocation.get().getAllocationEndDate());
            details.put("daysRemaining", daysRemaining);
        }

        details.put("impactLevel", calculateResourceImpactLevel(resourceId, projectId));

    } catch (Exception e) {
        details.put("error", "Unable to calculate impact details");
    }

    return details;
}

/**
 * Simple delivery impact validation before role-off confirmation
 * Returns warning message if risks identified, null if no risks
 */
public String validateDeliveryImpact(RoleOffRequestDTO dto) {
    try {
        // Get resource info for utilization analysis
        Resource resource = resourceRepo.findById(dto.getResourceId())
                .orElseThrow(() -> new RuntimeException("Resource not found"));

        // 1. Check Project Criticality
        String projectPriority = roleOffRepo.getProjectPriorityLevel(dto.getProjectId());
        boolean isCritical = "HIGH".equals(projectPriority) || "CRITICAL".equals(projectPriority);

        // 2. Check Remaining Demand
        long openDemand = roleOffRepo.countOpenDemandForProject(dto.getProjectId());

        // 3. Check Allocation Gap
        long currentAllocations = roleOffRepo.countActiveAllocationsForProject(dto.getProjectId());
        List<Integer> requiredPositionsList = roleOffRepo.getRequiredPositionsForProject(dto.getProjectId());
        int totalRequiredPositions = requiredPositionsList.stream().mapToInt(Integer::intValue).sum();
        int gapAfterRoleOff = Math.max(0, totalRequiredPositions - (int)(currentAllocations - 1));

        // 4. UTILIZATION & CAPACITY ANALYSIS
        // Current utilization for this specific project
        List<Integer> projectAllocations = roleOffRepo.getCurrentUtilization(dto.getResourceId());
        int currentProjectUtilization = projectAllocations.stream()
                .filter(alloc -> alloc != null)
                .findFirst()
                .orElse(0);

        // Total utilization across all projects
        Integer totalCurrentUtilization = roleOffRepo.getTotalCurrentUtilization(dto.getResourceId());
        int currentUtilization = totalCurrentUtilization != null ? totalCurrentUtilization : 0;

        // Utilization after role-off
        int utilizationAfterRoleOff = Math.max(0, currentUtilization - currentProjectUtilization);

        // Future capacity available
        int futureCapacity = 100 - utilizationAfterRoleOff;

        // Determine impact level
        String utilizationImpact = determineUtilizationImpact(currentUtilization, utilizationAfterRoleOff);
        String recommendation = generateRecommendation(utilizationImpact, futureCapacity, openDemand > 0);

        // Build comprehensive warning message
        StringBuilder warning = new StringBuilder();
        warning.append("Role-Off Impact Preview\n\n");
        warning.append("Resource: ").append(resource.getFullName()).append("\n");
        warning.append("Current Utilization: ").append(currentUtilization).append("%\n");
        warning.append("Utilization After Role-Off: ").append(utilizationAfterRoleOff).append("%\n\n");
        warning.append("Future Capacity Available: ").append(futureCapacity).append("%\n\n");

        // Add project risks if any
        if (isCritical || openDemand > 0 || gapAfterRoleOff > 0) {
            warning.append("Project Impact:\n");
            if (isCritical) {
                warning.append("- Project Criticality: ").append(projectPriority).append("\n");
            }
            if (openDemand > 0) {
                warning.append("- Remaining Demand: ").append(openDemand).append(" open positions\n");
            }
            if (gapAfterRoleOff > 0) {
                warning.append("- Resource Gap: ").append(gapAfterRoleOff).append(" position(s)\n");
            }
            warning.append("\n");
        }

        warning.append("Impact: ").append(utilizationImpact).append("\n");
        warning.append("Recommendation: ").append(recommendation).append("\n\n");
        warning.append("Do you want to proceed?");

        return warning.toString();

    } catch (Exception e) {
        // If validation fails, allow proceed with caution
        return "Unable to validate delivery impact. Proceed with caution.";
    }
}

/**
 * Determine utilization impact level based on before/after percentages
 */
private String determineUtilizationImpact(int currentUtilization, int utilizationAfterRoleOff) {
    int utilizationDrop = currentUtilization - utilizationAfterRoleOff;

    if (utilizationDrop >= 50) {
        return "Large utilization drop";
    } else if (utilizationDrop >= 25) {
        return "Moderate utilization drop";
    } else if (utilizationDrop >= 10) {
        return "Small utilization drop";
    } else if (utilizationDrop > 0) {
        return "Minimal utilization drop";
    } else {
        return "No utilization impact";
    }
}

/**
 * Generate recommendation based on utilization impact and future capacity
 */
private String generateRecommendation(String utilizationImpact, int futureCapacity, boolean hasOpenDemand) {
    if (utilizationImpact.equals("Large utilization drop")) {
        if (futureCapacity >= 50) {
            return "Assign resource to another demand immediately";
        } else {
            return "Urgent: Find replacement assignment";
        }
    } else if (utilizationImpact.equals("Moderate utilization drop")) {
        if (futureCapacity >= 30) {
            return "Assign resource to another demand";
        } else {
            return "Plan for replacement assignment";
        }
    } else if (utilizationImpact.equals("Small utilization drop")) {
        return "Monitor for new demand opportunities";
    } else if (hasOpenDemand) {
        return "Resource can fill existing demand";
    } else {
        return "No immediate action required";
    }
}

/**
 * Logs role-off decision for audit purposes
 */
public void logRoleOffDecision(RoleOffRequestDTO dto, String warning, boolean confirmed, Long userId) {
    String logMessage = String.format(
            "Role-off decision logged - Project: %d, Resource: %d, User: %d, " +
                    "Warning: %s, Confirmed: %s, Timestamp: %s",
            dto.getProjectId(),
            dto.getResourceId(),
            userId,
            warning != null ? "YES" : "NO",
            confirmed,
            LocalDateTime.now()
    );

    // In real implementation, save to audit table
    System.out.println("AUDIT LOG: " + logMessage);
}
//
//    private String getDeliveryOwnerName(Long deliveryOwnerId) {
//        if (deliveryOwnerId == null) {
//            return "N/A";
//        }
//        Optional<Resource> deliveryOwner = resourceRepo.findById(deliveryOwnerId);
//        return deliveryOwner.map(Resource::getFullName).orElse("N/A");
//    }

@Override
public ResponseEntity<?> getResources(Long pmId, Long projectId) {
    List<ResourceAllocation> allocations = roleOffRepo.findResources(projectId, pmId);
    return buildResourcesResponse(allocations);
}

/**
 * Common method to build resources response with optimized data fetching
 */
private ResponseEntity<?> buildResourcesResponse(List<ResourceAllocation> allocations) {

    if (allocations.isEmpty()) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Resources retrieved successfully!", Collections.emptyList()));
    }

    // Extract IDs
    List<UUID> allocationIds = allocations.stream()
            .map(ResourceAllocation::getAllocationId)
            .collect(Collectors.toList());

    List<Long> resourceIds = allocations.stream()
            .map(ra -> ra.getResource().getResourceId())
            .collect(Collectors.toList());

    // Fetch role-off events
    List<RoleOffEvent> roleOffEvents = roleOffRepo.findByAllocation_AllocationIdIn(allocationIds);

    // 🔥 Single pass processing instead of multiple maps
    Map<UUID, RoleOffEvent> roleOffMap = roleOffEvents.stream()
            .collect(Collectors.toMap(
                    roe -> roe.getAllocation().getAllocationId(),
                    Function.identity(),
                    (existing, replacement) -> existing
            ));

    // Skills mapping (optimized)
    Map<Long, List<String>> skillMap = mapToGroupedList(
            resourceSkillRepo.findSkillsByResourceIds(resourceIds)
    );

    Map<Long, List<String>> subSkillMap = mapToGroupedList(
            resourceSubSkillRepo.findSubSkillsByResourceIds(resourceIds)
    );

    // Project ID (safe extraction)
    Long projectId = Optional.ofNullable(allocations.get(0).getProject())
            .map(Project::getPmsProjectId)
            .orElse(null);

    // Impact calculation
    Map<Long, String> impactLevelsMap = calculateBatchImpactLevels(resourceIds, projectId);

    // DTO Mapping
    List<ResourcesDTO> dtos = allocations.stream()
            .map(ra -> {
                RoleOffEvent event = roleOffMap.get(ra.getAllocationId());
                Long resId = ra.getResource().getResourceId();

                return ResourcesDTO.builder()
                        .roleOffId(event != null ? event.getId() : null)
                        .resourceId(resId)
                        .name(ra.getResource().getFullName())
                        .department(ra.getResource().getDesignation()) // Mapping designation to department field as per ResourcesDTO
                        .projectName(ra.getProject().getName())
                        .clientName(ra.getProject().getClient().getClientName())
                        .deliveryRoleId(ra.getDemand() != null ? ra.getDemand().getRole().getId() : null)
                        .demandName(ra.getDemand() != null ? ra.getDemand().getDemandName() : null)
                        .skills(skillMap.getOrDefault(resId, Collections.emptyList()))
                        .subSkills(subSkillMap.getOrDefault(resId, Collections.emptyList()))
                        .allocationId(ra.getAllocationId())
                        .impact(impactLevelsMap.getOrDefault(resId, "LOW"))
                        .status(ra.getAllocationStatus())
                        .roleOffStatus(event != null ? event.getRoleOffStatus() : null)
                        .allocationPercentage(ra.getAllocationPercentage())
                        .endDate(ra.getAllocationEndDate())
                        .effectiveDate(event != null ? event.getEffectiveRoleOffDate() : null)
                        .rejectedBy(event != null ? event.getRejectedBy() : null)
                        .rejectionReason(event != null ? event.getRejectionReason() : null)
                        .build();
            })
            .collect(Collectors.toList());

    return ResponseEntity.ok(new ApiResponse<>(true, "Resources retrieved successfully!", dtos));
}

/**
 * Helper method to group repository results (List<Object[]>) into a Map<Long, List<String>>
 */
private Map<Long, List<String>> mapToGroupedList(List<Object[]> results) {
    if (results == null || results.isEmpty()) {
        return Collections.emptyMap();
    }
    return results.stream()
            .filter(obj -> obj.length >= 2 && obj[0] != null && obj[1] != null)
            .collect(Collectors.groupingBy(
                    obj -> (Long) obj[0],
                    Collectors.mapping(obj -> (String) obj[1], Collectors.toList())
            ));
}

    public ResponseEntity<?> getRoleOffKPI(Long projectId, Long managerId) {

        System.out.println("====== KPI DEBUG ======");
        System.out.println("projectId received: " + projectId);
        System.out.println("managerId received: " + managerId);

        // Raw native query to confirm
        List<Object[]> raw = roleOffRepo.debugCountByProject(projectId, managerId);
        raw.forEach(r -> System.out.println("status=" + r[0] + " count=" + r[1]));

        // 🔥 FIX: Get current active allocations first for consistent counting
        List<ResourceAllocation> currentAllocations = roleOffRepo.findResources(projectId, managerId);
        List<UUID> allocationIds = currentAllocations.stream()
                .map(ResourceAllocation::getAllocationId)
                .collect(Collectors.toList());

        Long active = roleOffRepo.countActive(projectId, managerId);

        // Count role-offs only for current active allocations
        Long pending = 0L;
        Long completed = 0L;
        if (!allocationIds.isEmpty()) {
            pending = roleOffRepo.countByAllocationIdsAndStatus(allocationIds, RoleOffStatus.PENDING);
            completed = roleOffRepo.countByAllocationIdsAndStatus(allocationIds, RoleOffStatus.FULFILLED);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("activeAllocations", active);
        data.put("pendingRoleOffs", pending);
        data.put("totalRoleOff", completed);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Role-off KPI retrieved successfully!",
                "data", data
        ));
    }
    @Override
    public ResponseEntity<?> getRMRoleOffEvents(Long rmId) {
        List<RoleOffEvent> roleOffEvents = roleOffRepo.findPendingRoleOffs(rmId, RoleOffStatus.PENDING);
        
        if (roleOffEvents.isEmpty()) {
            return ResponseEntity.ok(new ApiResponse<>(true, "Role-off events retrieved successfully!", List.of()));
        }

    // Batch fetch resource IDs for impact calculation
    List<Long> resourceIds = roleOffEvents.stream()
            .map(r -> r.getResource().getResourceId())
            .distinct()
            .toList();

    // Get project ID from first role-off event for impact calculation
    Long projectId = roleOffEvents.get(0).getProject().getPmsProjectId();

    // Batch calculate impact levels for all resources
    Map<Long, String> impactLevelsMap = calculateBatchImpactLevels(resourceIds, projectId);

    List<ResourcesDTO> dtos = roleOffEvents.stream().map(r -> {
        var allocation = r.getAllocation();
        var resource = r.getResource();
        var project = r.getProject();
        var client = project != null ? project.getClient() : null;
        var demand = allocation != null ? allocation.getDemand() : null;
        var role = demand != null ? demand.getRole() : null;

        // Skills List
        List<String> skills = role != null && role.getSkill() != null
                ? List.of(role.getSkill().getName())
                : Collections.emptyList();

        // SubSkills List
        List<String> subSkills = role != null && role.getSubSkill() != null
                ? List.of(role.getSubSkill().getName())
                : Collections.emptyList();

        return new ResourcesDTO(
                r.getId(),
                resource != null ? resource.getResourceId() : null,
                resource != null ? resource.getFullName() : "N/A",
                resource != null ? resource.getDesignation() : "N/A",
                project != null ? project.getName() : "N/A",
                client != null ? client.getClientName() : "N/A",
                role != null ? role.getId() : null,
                demand != null ? demand.getDemandName() : null,
                skills,
                subSkills,
                allocation != null ? allocation.getAllocationId() : null,
                impactLevelsMap.getOrDefault(resource != null ? resource.getResourceId() : null, "LOW"),
                allocation != null ? allocation.getAllocationStatus() : null,
                r.getRoleOffStatus(),
                allocation != null ? allocation.getAllocationPercentage() : null,
                allocation != null ? allocation.getAllocationEndDate() : null,
                r.getEffectiveRoleOffDate(),
                r.getRejectedBy(),
                r.getRejectionReason()
        );

    }).toList();

    return ResponseEntity.ok(new ApiResponse<>(true, "Role-off events retrieved successfully!", dtos));
}

/**
 * Generate impact preview for role-off confirmation
 */
private ResponseEntity<?> generateImpactPreview(com.dto.allocation_dto.RoleOffRequestDTO dto, Long userId,
                                                Resource resource, Project project, ResourceAllocation allocation) {

    // Calculate impact details
    String impactLevel = calculateResourceImpactLevel(resource.getResourceId(), project.getPmsProjectId());
    Map<String, Object> impactDetails = getImpactScoreDetails(resource.getResourceId(), project.getPmsProjectId());

    // Get utilization details
    Integer currentUtilization = allocation.getAllocationPercentage();
    Integer utilizationAfterRoleOff = 0;
    Integer futureCapacityAvailable = 100 - currentUtilization;

    // Get project details
    String projectCriticality = (String) impactDetails.get("projectPriority");
    Integer resourceGap = (Integer) impactDetails.get("resourceGap");

    // Build warning message
    StringBuilder warning = new StringBuilder();
    warning.append("Role-Off Impact Preview\n\n");
    warning.append("Resource: ").append(resource.getFullName()).append("\n");
    warning.append("Current Utilization: ").append(currentUtilization).append("%\n");
    warning.append("Utilization After Role-Off: ").append(utilizationAfterRoleOff).append("%\n\n");
    warning.append("Future Capacity Available: ").append(futureCapacityAvailable).append("%\n\n");

    warning.append("Project Impact:\n");
    warning.append("- Project Criticality: ").append(projectCriticality != null ? projectCriticality : "NORMAL").append("\n");
    warning.append("- Remaining Demand: ").append(resourceGap != null ? resourceGap : 0).append(" open positions\n");
    warning.append("- Resource Gap: ").append(resourceGap != null && resourceGap > 0 ? resourceGap + " position(s)" : "0 position(s)").append("\n\n");

    warning.append("Impact: ");
    if (currentUtilization >= 80) {
        warning.append("Large utilization drop\n");
    } else if (currentUtilization >= 50) {
        warning.append("Medium utilization drop\n");
    } else {
        warning.append("Small utilization drop\n");
    }

    warning.append("Recommendation: ");
    if ("HIGH".equals(impactLevel)) {
        warning.append("Urgent: Find replacement assignment\n");
    } else if ("MEDIUM".equals(impactLevel)) {
        warning.append("Recommended: Find replacement assignment\n");
    } else {
        warning.append("Optional: Monitor project impact\n");
    }

    warning.append("\n\nDo you want to proceed?");

    // Build response
    Map<String, Object> response = new HashMap<>();
    response.put("requiresConfirmation", true);
    response.put("warning", warning.toString());
    response.put("message", "Please review the role-off impact and confirm to proceed");
    response.put("impactLevel", impactLevel);
    response.put("resourceId", resource.getResourceId());
    response.put("projectId", project.getPmsProjectId());
    response.put("allocationId", allocation.getAllocationId());

    return ResponseEntity.ok(response);
}

/**
 * Process the actual role-off after confirmation
 */
private void processRoleOff(com.dto.allocation_dto.RoleOffRequestDTO dto, Long userId,
                            Resource resource, Project project, ResourceAllocation allocation) {

    // Validate RoleOffType
    if (dto.getRoleOffType() == null) {
        throw new ProjectExceptionHandler(
                HttpStatus.BAD_REQUEST,
                "ROLE_OFF_TYPE_REQUIRED",
                "RoleOffType must be provided");
    }

    // Get role from allocation or replacement role
    DeliveryRoleExpectation roleToSet = null;

    if (dto.getAllocationId() != null) {
        ResourceAllocation tempAllocation = allocationRepository.findById(dto.getAllocationId()).orElse(null);
        if (tempAllocation != null && tempAllocation.getDemand() != null) {
            roleToSet = tempAllocation.getDemand().getRole();
        }
    }

    // Handle role for auto replacement
    if (Boolean.TRUE.equals(dto.getAutoReplacementRequired())) {
        if (roleToSet == null) {
            if (dto.getReplacementRoleId() != null) {
                roleToSet = deliveryRoleExpectationRepo.findById(dto.getReplacementRoleId())
                        .orElseThrow(() -> new ProjectExceptionHandler(
                                HttpStatus.NOT_FOUND,
                                "REPLACEMENT_ROLE_NOT_FOUND",
                                "Replacement role not found"));
            } else {
                throw new ProjectExceptionHandler(
                        HttpStatus.BAD_REQUEST,
                        "REPLACEMENT_ROLE_REQUIRED",
                        "Replacement role ID is required when auto replacement is enabled");
            }
        }
    } else {
        if (roleToSet == null && dto.getReplacementRoleId() != null) {
            roleToSet = deliveryRoleExpectationRepo.findById(dto.getReplacementRoleId())
                    .orElseThrow(() -> new ProjectExceptionHandler(
                            HttpStatus.NOT_FOUND,
                            "REPLACEMENT_ROLE_NOT_FOUND",
                            "Replacement role not found"));
        }
    }

    // Create event
    RoleOffEvent event = new RoleOffEvent();
    event.setProject(project);
    event.setResource(resource);
    event.setRole(roleToSet);
    event.setRoleOffType(dto.getRoleOffType());
    event.setRoleInitiatedBy("PROJECT-MANAGER");
    event.setCreatedBy(userId);
    event.setAllocation(allocation);

    event.setRoleOffReasonEnum(dto.getRoleOffReason());
    event.setEffectiveRoleOffDate(dto.getEffectiveRoleOffDate());

    // NEW FIELD: Map resourcePerformance from DTO to entity
    event.setResourcePerformance(dto.getResourcePerformance());

    // =========================================================
    // 🔥 EMERGENCY ROLE-OFF
    // =========================================================
    if (dto.getRoleOffType() == RoleOffType.EMERGENCY) {

        event.setRoleOffReason(dto.getEmergencyReason());
        event.setRoleOffStatus(RoleOffStatus.APPROVED);

        roleOffRepo.save(event);

        // Replacement
        if (Boolean.TRUE.equals(dto.getAutoReplacementRequired())) {
            createReplacementDemand(event, userId);
            event.setReplacementStatus(ReplacementStatus.AUTO_CREATED);
        } else {
            if (dto.getSkipReason() == null || dto.getSkipReason().isBlank()) {
                throw new ProjectExceptionHandler(
                        HttpStatus.BAD_REQUEST,
                        "SKIP_REASON_REQUIRED",
                        "Skip reason required when replacement is disabled");
            }
            event.setReplacementStatus(ReplacementStatus.SKIPPED);
            event.setSkipReason(dto.getSkipReason());
        }

        roleOffRepo.save(event);

        // Immediate execution
        closeResourceAllocation(event);

        event.setRoleOffStatus(RoleOffStatus.FULFILLED);
        roleOffRepo.save(event);

        return;
    }

    // =========================================================
    // 🔥 PLANNED ROLE-OFF
    // =========================================================
    if (dto.getRoleOffType() == RoleOffType.PLANNED) {

        if (dto.getEffectiveRoleOffDate() == null) {
            throw new ProjectExceptionHandler(
                    HttpStatus.BAD_REQUEST,
                    "ROLE_OFF_DATE_REQUIRED",
                    "Role-off date required");
        }

        if (dto.getEffectiveRoleOffDate().isBefore(LocalDate.now())) {
            throw new ProjectExceptionHandler(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_ROLE_OFF_DATE",
                    "Role-off date cannot be in past");
        }

        // Project timeline validation
        if (project.getEndDate().toLocalDate().isBefore(dto.getEffectiveRoleOffDate())) {
            throw new ProjectExceptionHandler(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_PROJECT_TIMELINE",
                    "Project end date cannot be before role-off date");
        }

        event.setRoleOffReason(dto.getRoleOffReason().name());
        event.setRoleOffStatus(RoleOffStatus.PENDING);

        roleOffRepo.save(event);

        // ✅ SINGLE replacement logic (FIXED)
        if (Boolean.TRUE.equals(dto.getAutoReplacementRequired())) {
            createReplacementDemand(event, userId);
            event.setReplacementStatus(ReplacementStatus.AUTO_CREATED);
        } else {
            if (dto.getSkipReason() == null || dto.getSkipReason().isBlank()) {
                throw new ProjectExceptionHandler(
                        HttpStatus.BAD_REQUEST,
                        "SKIP_REASON_REQUIRED",
                        "Skip reason required when replacement is disabled");
            }
            event.setReplacementStatus(ReplacementStatus.SKIPPED);
            event.setSkipReason(dto.getSkipReason());
        }

        roleOffRepo.save(event);
    }
}

/**
 * Helper method to create replacement demand for a role-off event
 */
private void createReplacementDemand(RoleOffEvent event, Long userId) {
    try {
        if (event.getAllocation() != null && event.getRole() != null) {
            ResourceAllocation allocation = event.getAllocation();

            // Create replacement demand starting from role-off effective date
            LocalDate startDate = event.getEffectiveRoleOffDate();
            LocalDate endDate = allocation.getAllocationEndDate();

            demandService.createReplacementDemandFromAllocation(allocation, startDate, endDate, userId);

            log.info("Replacement demand created for role-off event: {}", event.getId());
        }
    } catch (Exception e) {
        log.error("Failed to create replacement demand for role-off event {}: {}",
                 event.getId(), e.getMessage());
        throw new ProjectExceptionHandler(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "REPLACEMENT_DEMAND_CREATION_FAILED",
                "Failed to create replacement demand: " + e.getMessage());
    }
}

/**
 * Helper method to close resource allocation for role-off
 */
private void closeResourceAllocation(RoleOffEvent event) {
    try {
        if (event.getAllocation() != null) {
            ResourceAllocation allocation = event.getAllocation();

            // Create close allocation DTO
            CloseAllocationDTO closeDTO = new CloseAllocationDTO();
            closeDTO.setClosureDate(event.getEffectiveRoleOffDate());
            closeDTO.setReason("ROLE_OFF");

            // Close the allocation using allocation service
            allocationService.closeAllocation(allocation.getAllocationId(), closeDTO);

            log.info("Allocation {} closed due to role-off {}",
                     allocation.getAllocationId(), event.getId());
        }
    } catch (Exception e) {
        log.error("Failed to close allocation for role-off event {}: {}",
                 event.getId(), e.getMessage());
        throw new ProjectExceptionHandler(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "ALLOCATION_CLOSURE_FAILED",
                "Failed to close allocation: " + e.getMessage());
    }
}

// ========== SEPARATE APPROVE/REJECT METHODS FOR RESOURCE MANAGER ==========

@Override
@Transactional
public ResponseEntity<?> rmApprove(UUID id, UserDTO userDTO) {
    RoleOffEvent event = roleOffRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("RoleOff event with ID " + id + " not found. The event may have been deleted or the ID is incorrect."));

    if (event.getRoleOffStatus() != RoleOffStatus.PENDING) {
        throw new RuntimeException("Already processed");
    }

    event.setRmApproved(true);
    event.setRoleOffStatus(RoleOffStatus.APPROVED);

    roleOffRepo.save(event);

    return ResponseEntity.ok(new ApiResponse<>(true, "Approved by RM → Waiting for DL", null));
}

@Override
@Transactional
public ResponseEntity<?> rmReject(UUID id, String rejectionReason, UserDTO userDTO) {
    RoleOffEvent event = roleOffRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("RoleOff event with ID " + id + " not found. The event may have been deleted or the ID is incorrect."));

    if (event.getRoleOffStatus() != RoleOffStatus.PENDING) {
        throw new RuntimeException("Already processed");
    }

    if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
        throw new RuntimeException("Rejection reason is required");
    }

    event.setRoleOffStatus(RoleOffStatus.REJECTED);
    event.setRejectedBy("RESOURCE-MANAGER");
    event.setRejectionReason(rejectionReason.trim());

    roleOffRepo.save(event);
    return ResponseEntity.ok(new ApiResponse<>(true, "Rejected by RM", null));
}

// ========== SEPARATE FULFILL/REJECT METHODS FOR DELIVERY MANAGER ==========

    @Override
    @Transactional
    public ResponseEntity<?> dlFulfill(UUID id, UserDTO userDTO) {

        RoleOffEvent event = roleOffRepo.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "RoleOff event with ID " + id + " not found."));

    // RM must approve first
    if (event.getRmApproved() == null || !event.getRmApproved()) {
        throw new RuntimeException("RM approval required first");
    }

    if (event.getRoleOffStatus() != RoleOffStatus.APPROVED) {
        throw new RuntimeException("Invalid state - role-off must be RM approved first");
    }

        event.setDlApproved(true);
        event.setDlActionDate(LocalDate.now());
        event.setRoleOffStatus(RoleOffStatus.FULFILLED);

        ResourceAllocation allocation = event.getAllocation();

        // ✅ ALWAYS set end date
        allocation.setAllocationEndDate(event.getEffectiveRoleOffDate());

        // 🔥 CRITICAL FIX → ALWAYS END allocation
        allocation.setAllocationStatus(AllocationStatus.ENDED);

        allocationRepository.save(allocation);

        // Keep for ledger / async updates
        closeResourceAllocation(event);

        roleOffRepo.save(event);

        return ResponseEntity.ok("Role-off fulfilled successfully");
    }

@Override
@Transactional
public ResponseEntity<?> dlReject(UUID id, String rejectionReason, UserDTO userDTO) {
    RoleOffEvent event = roleOffRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("RoleOff event with ID " + id + " not found. The event may have been deleted or the ID is incorrect."));

    // RM must approve first
    if (event.getRmApproved() == null || !event.getRmApproved()) {
        throw new RuntimeException("RM approval required first");
    }

    if (event.getRoleOffStatus() != RoleOffStatus.APPROVED) {
        throw new RuntimeException("Invalid state");
    }

    if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
        throw new RuntimeException("Rejection reason is required");
    }

    event.setRoleOffStatus(RoleOffStatus.REJECTED);
    event.setRejectedBy("DELIVERY-MANAGER");
    event.setRejectionReason(rejectionReason.trim());

    roleOffRepo.save(event);
    return ResponseEntity.ok(new ApiResponse<>(true, "Rejected by DL", null));
}

@Override
public ResponseEntity<?> getDMRoleOffEvents(Long dmId) {
    List<RoleOffEvent> roleOffEvents = roleOffRepo.findPendingRoleOffsDm(dmId, RoleOffStatus.APPROVED);

    if (roleOffEvents.isEmpty()) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Role-off events retrieved successfully!", List.of()));
    }

    // Batch fetch resource IDs for impact calculation
    List<Long> resourceIds = roleOffEvents.stream()
            .map(r -> r.getResource().getResourceId())
            .distinct()
            .toList();

    // Get project ID from first role-off event for impact calculation
    Long projectId = roleOffEvents.get(0).getProject().getPmsProjectId();

    // Batch calculate impact levels for all resources
    Map<Long, String> impactLevelsMap = calculateBatchImpactLevels(resourceIds, projectId);

    List<ResourcesDTO> dtos = roleOffEvents.stream().map(r -> {
        var allocation = r.getAllocation();
        var resource = r.getResource();
        var project = r.getProject();
        var client = project != null ? project.getClient() : null;
        var demand = allocation != null ? allocation.getDemand() : null;
        var role = demand != null ? demand.getRole() : null;

        // Skills List
        List<String> skills = role != null && role.getSkill() != null
                ? List.of(role.getSkill().getName())
                : Collections.emptyList();

        // SubSkills List
        List<String> subSkills = role != null && role.getSubSkill() != null
                ? List.of(role.getSubSkill().getName())
                : Collections.emptyList();

        return new ResourcesDTO(
                r.getId(),
                resource != null ? resource.getResourceId() : null,
                resource != null ? resource.getFullName() : "N/A",
                resource != null ? resource.getDesignation() : "N/A",
                project != null ? project.getName() : "N/A",
                client != null ? client.getClientName() : "N/A",
                role != null ? role.getId() : null,
                demand != null ? demand.getDemandName() : null,
                skills,
                subSkills,
                allocation != null ? allocation.getAllocationId() : null,
                impactLevelsMap.getOrDefault(resource != null ? resource.getResourceId() : null, "LOW"),
                allocation != null ? allocation.getAllocationStatus() : null,
                r.getRoleOffStatus(),
                allocation != null ? allocation.getAllocationPercentage() : null,
                allocation != null ? allocation.getAllocationEndDate() : null,
                r.getEffectiveRoleOffDate(),
                r.getRejectedBy(),
                r.getRejectionReason()
        );

    }).toList();

    return ResponseEntity.ok(new ApiResponse<>(true, "Role-off events retrieved successfully!", dtos));
}

@Override
public ResponseEntity<?> pmCancel(UUID id, UserDTO userDTO) {
    // ========== VALIDATION ==========

    // 1. Basic validation
    if (id == null) {
        return ResponseEntity.badRequest().body("Role-off event ID is required");
    }

    // 2. Find role-off event
    Optional<RoleOffEvent> roleOffEventOpt = roleOffRepo.findById(id);
    if (roleOffEventOpt.isEmpty()) {
        return ResponseEntity.badRequest().body("Role-off event not found");
    }

    RoleOffEvent event = roleOffEventOpt.get();

    // 3. Validate current status - only PENDING or APPROVED can be cancelled
    if (event.getRoleOffStatus() != RoleOffStatus.PENDING && event.getRoleOffStatus() != RoleOffStatus.APPROVED) {
        return ResponseEntity.badRequest().body("Only PENDING or APPROVED role-off requests can be cancelled");
    }

    // 4. Validate project ownership - PM can only cancel role-offs for their projects
    if (event.getProject() == null || event.getProject().getProjectManagerId() == null) {
        return ResponseEntity.badRequest().body("Invalid role-off event: missing project information");
    }

    if (!event.getProject().getProjectManagerId().equals(userDTO.getId())) {
        return ResponseEntity.badRequest().body("You can only cancel role-off requests for your own projects");
    }

    // 5. Business validation - cannot cancel if already fulfilled or rejected
    if (event.getRoleOffStatus() == RoleOffStatus.FULFILLED) {
        return ResponseEntity.badRequest().body("Cannot cancel a role-off request that has already been fulfilled");
    }

    if (event.getRoleOffStatus() == RoleOffStatus.CANCELLED) {
        return ResponseEntity.badRequest().body("Role-off request is already cancelled");
    }

    // ========== EXECUTION ==========

    try {
        // Log the cancellation for audit purposes
        logRoleOffCancellation(event, "Cancelled by Project Manager", userDTO.getId());

        // Delete the role-off event instead of just updating status
        roleOffRepo.delete(event);

        return ResponseEntity.ok(new ApiResponse<>(true,
                "Role-off request cancelled and deleted successfully",
                Map.of(
                        "eventId", id,
                        "status", "DELETED",
                        "cancelledBy", "PROJECT-MANAGER"
                )));

    } catch (Exception e) {
        return ResponseEntity.internalServerError().body("Error deleting role-off request: " + e.getMessage());
    }
}

/**
 * Log role-off deletion for audit purposes
 */
private void logRoleOffDeletion(RoleOffEvent event, String reason) {
    String logMessage = String.format(
            "Role-off DELETED - Event ID: %s, Project: %s, Resource: %s, " +
                    "Previous Status: %s, Reason: %s, Timestamp: %s",
            event.getId(),
            event.getProject() != null ? event.getProject().getName() : "N/A",
            event.getResource() != null ? event.getResource().getFullName() : "N/A",
            event.getRoleOffStatus(),
            reason,
            LocalDateTime.now()
    );

    // In real implementation, save to audit table
    System.out.println("AUDIT LOG: " + logMessage);
}

/**
 * Log role-off cancellation for audit purposes
 */
private void logRoleOffCancellation(RoleOffEvent event, String reason, Long userId) {
    String logMessage = String.format(
            "Role-off CANCELLED - Event ID: %s, Project: %s, Resource: %s, User: %d, " +
                    "Reason: %s, Timestamp: %s",
            event.getId(),
            event.getProject() != null ? event.getProject().getName() : "N/A",
            event.getResource() != null ? event.getResource().getFullName() : "N/A",
            userId,
            reason,
            LocalDateTime.now()
    );

    // In real implementation, save to audit table
    System.out.println("AUDIT LOG: " + logMessage);
}

// ========== BULK ROLE-OFF METHODS FOR PLANNED ROLE TYPE ==========

@Override
@Transactional
public ResponseEntity<?> bulkPlannedRoleOff(BulkRoleOffRequestDTO bulkRequest, UserDTO userDTO) {

    // Validation
    if (!bulkRequest.isValidForBulkPlannedRoleOff()) {
        return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, "Invalid bulk role-off request. Please check all required fields.", null));
    }

    // Validate that this is PLANNED role type (emergency not allowed for bulk)
    if (bulkRequest.getEffectiveRoleOffDate().isBefore(LocalDate.now())) {
        return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, "Bulk role-off date cannot be in the past for planned role-offs.", null));
    }

    // Validate Project
    Project project = projectRepo.findById(bulkRequest.getProjectId())
            .orElseThrow(() -> new ProjectExceptionHandler(
                    HttpStatus.NOT_FOUND,
                    "PROJECT_NOT_FOUND",
                    "Project not found"));

    // Fetch all allocations
    List<ResourceAllocation> allocations = allocationRepository.findAllById(bulkRequest.getAllocationIds());

    if (allocations.isEmpty()) {
        return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, "No valid allocations found", null));
    }

    List<RoleOffEvent> successEvents = new ArrayList<>();
    List<Map<String, Object>> failedEvents = new ArrayList<>();

    // Impact preview mode (if not confirmed)
    if (bulkRequest.getConfirmed() == null || !bulkRequest.getConfirmed()) {
        return generateBulkImpactPreview(bulkRequest, userDTO, project, allocations);
    }

    // Process bulk role-off
    for (ResourceAllocation allocation : allocations) {
        try {
            // Validate allocation belongs to the project
            if (!allocation.getProject().getPmsProjectId().equals(project.getPmsProjectId())) {
                failedEvents.add(Map.of(
                        "allocationId", allocation.getAllocationId(),
                        "reason", "Allocation does not belong to specified project"
                ));
                continue;
            }

            // Validate allocation is active
            if (allocation.getAllocationStatus() != AllocationStatus.ACTIVE) {
                failedEvents.add(Map.of(
                        "allocationId", allocation.getAllocationId(),
                        "reason", "Allocation is not active"
                ));
                continue;
            }

            // Check for existing role-off
            RoleOffEvent existingRoleOff = roleOffRepo.findByAllocation_AllocationId(allocation.getAllocationId());
            if (existingRoleOff != null) {
                if (existingRoleOff.getRoleOffStatus() == RoleOffStatus.REJECTED) {
                    roleOffRepo.delete(existingRoleOff);
                } else {
                    failedEvents.add(Map.of(
                            "allocationId", allocation.getAllocationId(),
                            "reason", "Role-off already exists with status: " + existingRoleOff.getRoleOffStatus()
                    ));
                    continue;
                }
            }

            // Create role-off event
            RoleOffEvent event = createBulkRoleOffEvent(bulkRequest, userDTO, project, allocation);
            successEvents.add(event);

        } catch (Exception ex) {
            failedEvents.add(Map.of(
                    "allocationId", allocation.getAllocationId(),
                    "reason", ex.getMessage()
            ));
        }
    }

    // Save all successful events
    List<RoleOffEvent> savedEvents = roleOffRepo.saveAll(successEvents);

    // Build response
    Map<String, Object> result = new HashMap<>();
    result.put("successCount", savedEvents.size());
    result.put("failedCount", failedEvents.size());
    result.put("success", savedEvents);
    result.put("failed", failedEvents);

    String message;
    if (!savedEvents.isEmpty() && !failedEvents.isEmpty()) {
        message = savedEvents.size() + " bulk role-off requests created, " + failedEvents.size() + " failed";
    } else if (!savedEvents.isEmpty()) {
        message = "All bulk role-off requests created successfully";
    } else {
        message = "All bulk role-off requests failed";
    }

    return ResponseEntity.ok().body(new ApiResponse<>(true, message, result));
}

private RoleOffEvent createBulkRoleOffEvent(BulkRoleOffRequestDTO bulkRequest, UserDTO userDTO,
                                            Project project, ResourceAllocation allocation) {
    RoleOffEvent event = new RoleOffEvent();

    event.setProject(project);
    event.setAllocation(allocation);
    event.setResource(allocation.getResource());
    event.setRoleOffType(RoleOffType.PLANNED); // Always PLANNED for bulk
    event.setEffectiveRoleOffDate(bulkRequest.getEffectiveRoleOffDate());
    event.setRoleOffStatus(RoleOffStatus.PENDING);
    event.setRoleOffReason(bulkRequest.getRoleOffReason().name());
    event.setRoleOffReasonEnum(bulkRequest.getRoleOffReason());
    event.setCreatedAt(LocalDate.now());
    event.setCreatedBy(userDTO.getId());
    event.setRoleInitiatedBy(
            userDTO.getRoles().contains("PROJECT-MANAGER") ? "PROJECT-MANAGER" :
                    userDTO.getRoles().contains("RESOURCE-MANAGER") ? "RESOURCE-MANAGER" : "Unknown"
    );

    // Set role from allocation's demand if available
    if (allocation.getDemand() != null && allocation.getDemand().getRole() != null) {
        event.setRole(allocation.getDemand().getRole());
    }

    // Auto-replacement is not allowed for bulk role-offs
    event.setReplacementStatus(ReplacementStatus.SKIPPED);
    event.setSkipReason("Auto-replacement not allowed for bulk role-offs");

    return event;
}

private ResponseEntity<?> generateBulkImpactPreview(BulkRoleOffRequestDTO bulkRequest, UserDTO userDTO,
                                                    Project project, List<ResourceAllocation> allocations) {

    // Calculate impact for all resources
    List<Long> resourceIds = allocations.stream()
            .map(a -> a.getResource().getResourceId())
            .toList();

    Map<Long, String> impactLevelsMap = calculateBatchImpactLevels(resourceIds, project.getPmsProjectId());

    // Build preview response
    List<Map<String, Object>> resourceImpacts = new ArrayList<>();
    int totalUtilization = 0;
    int highImpactCount = 0;

    for (ResourceAllocation allocation : allocations) {
        Long resourceId = allocation.getResource().getResourceId();
        String impactLevel = impactLevelsMap.getOrDefault(resourceId, "LOW");

        Map<String, Object> impact = new HashMap<>();
        impact.put("allocationId", allocation.getAllocationId());
        impact.put("resourceId", resourceId);
        impact.put("resourceName", allocation.getResource().getFullName());
        impact.put("currentUtilization", allocation.getAllocationPercentage());
        impact.put("impactLevel", impactLevel);
        impact.put("allocationPercentage", allocation.getAllocationPercentage());

        resourceImpacts.add(impact);

        totalUtilization += allocation.getAllocationPercentage() != null ? allocation.getAllocationPercentage() : 0;
        if ("HIGH".equals(impactLevel)) highImpactCount++;
    }

    // Build warning message
    StringBuilder warning = new StringBuilder();
    warning.append("Bulk Role-Off Impact Preview\n\n");
    warning.append("Project: ").append(project.getName()).append("\n");
    warning.append("Total Resources: ").append(allocations.size()).append("\n");
    warning.append("Total Utilization Impact: ").append(totalUtilization).append("%\n");
    warning.append("High Impact Resources: ").append(highImpactCount).append("\n\n");

    if (highImpactCount > 0) {
        warning.append("⚠️ WARNING: ").append(highImpactCount).append(" resources have HIGH impact on project delivery.\n");
        warning.append("Recommendation: Consider staggering role-offs or ensure replacements are ready.\n\n");
    }

    warning.append("Do you want to proceed with bulk role-off?");

    // Build response
    Map<String, Object> response = new HashMap<>();
    response.put("requiresConfirmation", true);
    response.put("warning", warning.toString());
    response.put("message", "Please review the bulk role-off impact and confirm to proceed");
    response.put("resourceImpacts", resourceImpacts);
    response.put("projectId", project.getPmsProjectId());
    response.put("totalResources", allocations.size());
    response.put("highImpactCount", highImpactCount);

    return ResponseEntity.ok(response);
}

@Override
@Transactional
public ResponseEntity<?> bulkRmApprove(List<UUID> ids, UserDTO userDTO) {
    List<RoleOffEvent> approvedEvents = new ArrayList<>();
    List<Map<String, Object>> failedEvents = new ArrayList<>();

    for (UUID id : ids) {
        try {
            RoleOffEvent event = roleOffRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("RoleOff event not found: " + id));

            if (event.getRoleOffStatus() != RoleOffStatus.PENDING) {
                failedEvents.add(Map.of(
                        "id", id,
                        "reason", "Event is not in PENDING status"
                ));
                continue;
            }

            event.setRmApproved(true);
            event.setRoleOffStatus(RoleOffStatus.APPROVED);
            approvedEvents.add(event);

        } catch (Exception ex) {
            failedEvents.add(Map.of(
                    "id", id,
                    "reason", ex.getMessage()
            ));
        }
    }

    if (!approvedEvents.isEmpty()) {
        roleOffRepo.saveAll(approvedEvents);
    }

    Map<String, Object> result = new HashMap<>();
    result.put("approvedCount", approvedEvents.size());
    result.put("failedCount", failedEvents.size());
    result.put("approved", approvedEvents);
    result.put("failed", failedEvents);

    String message = approvedEvents.size() + " role-off events approved by RM";
    return ResponseEntity.ok(new ApiResponse<>(true, message, result));
}

@Override
@Transactional
public ResponseEntity<?> bulkRmReject(List<UUID> ids, String rejectionReason, UserDTO userDTO) {
    if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
        return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, "Rejection reason is required for bulk rejection", null));
    }

    List<RoleOffEvent> rejectedEvents = new ArrayList<>();
    List<Map<String, Object>> failedEvents = new ArrayList<>();

    for (UUID id : ids) {
        try {
            RoleOffEvent event = roleOffRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("RoleOff event not found: " + id));

            if (event.getRoleOffStatus() != RoleOffStatus.PENDING) {
                failedEvents.add(Map.of(
                        "id", id,
                        "reason", "Event is not in PENDING status"
                ));
                continue;
            }

            event.setRoleOffStatus(RoleOffStatus.REJECTED);
            event.setRejectedBy("RESOURCE-MANAGER");
            event.setRejectionReason(rejectionReason.trim());
            rejectedEvents.add(event);

        } catch (Exception ex) {
            failedEvents.add(Map.of(
                    "id", id,
                    "reason", ex.getMessage()
            ));
        }
    }

    if (!rejectedEvents.isEmpty()) {
        roleOffRepo.saveAll(rejectedEvents);
    }

    Map<String, Object> result = new HashMap<>();
    result.put("rejectedCount", rejectedEvents.size());
    result.put("failedCount", failedEvents.size());
    result.put("rejected", rejectedEvents);
    result.put("failed", failedEvents);

    String message = rejectedEvents.size() + " role-off events rejected by RM";
    return ResponseEntity.ok(new ApiResponse<>(true, message, result));
}

@Override
@Transactional
public ResponseEntity<?> bulkDlFulfill(List<UUID> ids, UserDTO userDTO) {
    List<RoleOffEvent> fulfilledEvents = new ArrayList<>();
    List<Map<String, Object>> failedEvents = new ArrayList<>();

    for (UUID id : ids) {
        try {
            RoleOffEvent event = roleOffRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("RoleOff event not found: " + id));

            // RM must approve first
            if (event.getRmApproved() == null || !event.getRmApproved()) {
                failedEvents.add(Map.of(
                        "id", id,
                        "reason", "RM approval required first"
                ));
                continue;
            }

            if (event.getRoleOffStatus() != RoleOffStatus.APPROVED) {
                failedEvents.add(Map.of(
                        "id", id,
                        "reason", "Event is not in APPROVED status"
                ));
                continue;
            }

            event.setDlApproved(true);
            event.setDlActionDate(LocalDate.now());
            event.setRoleOffStatus(RoleOffStatus.FULFILLED);

            // Execute immediately if date has passed
            if (!event.getEffectiveRoleOffDate().isAfter(LocalDate.now())) {
                closeResourceAllocation(event);
            }

            fulfilledEvents.add(event);

        } catch (Exception ex) {
            failedEvents.add(Map.of(
                    "id", id,
                    "reason", ex.getMessage()
            ));
        }
    }

    if (!fulfilledEvents.isEmpty()) {
        roleOffRepo.saveAll(fulfilledEvents);
    }

    Map<String, Object> result = new HashMap<>();
    result.put("fulfilledCount", fulfilledEvents.size());
    result.put("failedCount", failedEvents.size());
    result.put("fulfilled", fulfilledEvents);
    result.put("failed", failedEvents);

    String message = fulfilledEvents.size() + " role-off events fulfilled by DL";
    return ResponseEntity.ok(new ApiResponse<>(true, message, result));
}

@Override
@Transactional
public ResponseEntity<?> bulkDlReject(List<UUID> ids, String rejectionReason, UserDTO userDTO) {
    if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
        return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, "Rejection reason is required for bulk rejection", null));
    }

    List<RoleOffEvent> rejectedEvents = new ArrayList<>();
    List<Map<String, Object>> failedEvents = new ArrayList<>();

    for (UUID id : ids) {
        try {
            RoleOffEvent event = roleOffRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("RoleOff event not found: " + id));

            // RM must approve first
            if (event.getRmApproved() == null || !event.getRmApproved()) {
                failedEvents.add(Map.of(
                        "id", id,
                        "reason", "RM approval required first"
                ));
                continue;
            }

            if (event.getRoleOffStatus() != RoleOffStatus.APPROVED) {
                failedEvents.add(Map.of(
                        "id", id,
                        "reason", "Event is not in APPROVED status"
                ));
                continue;
            }

            event.setRoleOffStatus(RoleOffStatus.REJECTED);
            event.setRejectedBy("DELIVERY-MANAGER");
            event.setRejectionReason(rejectionReason.trim());
            rejectedEvents.add(event);

        } catch (Exception ex) {
            failedEvents.add(Map.of(
                    "id", id,
                    "reason", ex.getMessage()
            ));
        }
    }

    if (!rejectedEvents.isEmpty()) {
        roleOffRepo.saveAll(rejectedEvents);
    }

    Map<String, Object> result = new HashMap<>();
    result.put("rejectedCount", rejectedEvents.size());
    result.put("failedCount", failedEvents.size());
    result.put("rejected", rejectedEvents);
    result.put("failed", failedEvents);

    String message = rejectedEvents.size() + " role-off events rejected by DL";
    return ResponseEntity.ok(new ApiResponse<>(true, message, result));
}

@Override
@Transactional
public void handleAttrition(Long resourceId, LocalDate dateOfExit, Long userId) {
    log.info("Processing attrition for resource ID: {} with exit date: {}", resourceId, dateOfExit);

    // Step 1: Fetch resource and check status
    Resource resource = resourceRepo.findById(resourceId)
            .orElseThrow(() -> new ProjectExceptionHandler(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Resource not found"));

    if (EmploymentStatus.EXITED.equals(resource.getEmploymentStatus())) {
        log.info("Resource {} already marked as EXITED. Skipping attrition processing.", resourceId);
        return;
    }

    // Step 2: Fetch all allocations for resource
    List<ResourceAllocation> allocations = allocationRepository.findByResource_ResourceId(resourceId);

    // Step 3: Process EACH allocation independently
    for (ResourceAllocation allocation : allocations) {
        // Only process ACTIVE or PLANNED allocations
        if (AllocationStatus.ENDED.equals(allocation.getAllocationStatus()) ||
                AllocationStatus.CANCELLED.equals(allocation.getAllocationStatus())) {
            continue;
        }

        LocalDate startDate = allocation.getAllocationStartDate();
        LocalDate endDate = allocation.getAllocationEndDate();

        // ✅ NULL CHECK
        if (startDate == null || endDate == null) {
            log.error("Skipping allocation {} due to null dates", allocation.getAllocationId());
            continue;
        }

        // ✅ INVALID RANGE CHECK
        if (startDate.isAfter(endDate)) {
            log.error("Skipping allocation {} due to invalid date range", allocation.getAllocationId());
            continue;
        }

        // CASE 1: Past allocation (allocationEndDate < dateOfExit) -> Do nothing
        if (endDate.isBefore(dateOfExit)) {
            log.debug("Skipping past allocation {} for attrition processing", allocation.getAllocationId());
            continue;
        }

        // CASE 2: OVERLAPPING allocation (allocationStartDate <= dateOfExit <= allocationEndDate)
        if (!startDate.isAfter(dateOfExit) && !endDate.isBefore(dateOfExit)) {
            log.info("Closing overlapping allocation {} due to attrition", allocation.getAllocationId());

            // 1. Close allocation using existing logic
            CloseAllocationDTO closeDTO = new CloseAllocationDTO();
            closeDTO.setClosureDate(dateOfExit);
            closeDTO.setReason("ATTRITION");
            allocationService.closeAllocation(allocation.getAllocationId(), closeDTO);

            // ✅ FIX: EXACT DATE MATCH - Skip demand creation if allocation ends on exit date
            if (endDate.isEqual(dateOfExit)) {
                log.info("Skipping demand creation as allocation ends on exit date for allocation {}",
                        allocation.getAllocationId());
                continue;
            }

            // Optional: skip past demand
            if (dateOfExit.isBefore(LocalDate.now())) {
                continue;
            }

            // 2. Create replacement demand: startDate = dateOfExit + 1, endDate = original allocationEndDate
            demandService.createReplacementDemandFromAllocation(allocation, dateOfExit.plusDays(1), endDate, userId);
        }

        // CASE 3: FUTURE allocation (allocationStartDate > dateOfExit)
        else if (startDate.isAfter(dateOfExit)) {
            log.info("Cancelling future allocation {} due to attrition", allocation.getAllocationId());

            // 1. Mark as CANCELLED
            allocation.setAllocationStatus(AllocationStatus.CANCELLED);
            allocation.setClosureReason("CANCELLED DUE TO RESOURCE ATTRITION");

            // 2. Save using existing repository
            allocationRepository.save(allocation);

            // 3. Create replacement demand: startDate = allocationStartDate, endDate = allocationEndDate
            if (!dateOfExit.isBefore(LocalDate.now())) {
                demandService.createReplacementDemandFromAllocation(allocation, startDate, endDate, userId);
            }
        }
    }

    // Step 6: Trigger ledger update
    // Using same horizon logic as closeResourceAllocation to maintain consistency
    LocalDate horizonEnd = LocalDate.now().plusDays(90);
    java.util.Optional<LocalDate> maxAllocEnd = allocationRepository.findMaxAllocationEndDateForResource(resourceId);
    if (maxAllocEnd.isPresent() && maxAllocEnd.get().isAfter(horizonEnd)) {
        horizonEnd = maxAllocEnd.get();
    }

    availabilityLedgerAsyncService.updateLedger(resourceId, dateOfExit, horizonEnd);

    log.info("Attrition processing completed for resource: {}", resource.getFullName());
}

@Override
@Transactional
public String removeResourceFromOrganization(ResourceRemovalDTO removalDTO, Long userId) {
    log.info("Processing resource removal from organization for resource ID: {}", removalDTO.getResourceId());

    try {
        // Step 1: Validate resource exists and is active
        Resource resource = resourceRepo.findById(removalDTO.getResourceId())
                .orElseThrow(() -> new ProjectExceptionHandler(
                        HttpStatus.NOT_FOUND,
                        "RESOURCE_NOT_FOUND",
                        "Resource not found with ID: " + removalDTO.getResourceId()));

        // Check if resource is already exited
        if (EmploymentStatus.EXITED.equals(resource.getEmploymentStatus())) {
            throw new ProjectExceptionHandler(
                    HttpStatus.BAD_REQUEST,
                    "RESOURCE_ALREADY_EXITED",
                    "Resource has already exited the organization");
        }

        // Step 2: Update resource status to ON_NOTICE and set notice period details
        resource.setEmploymentStatus(EmploymentStatus.ON_NOTICE);
        resource.setDateOfExit(removalDTO.getNoticePeriodEndDate());
        resource.setNoticeStartDate(LocalDate.now());
        resource.setNoticeEndDate(removalDTO.getNoticePeriodEndDate());
        resource.setChangedBy(userId);
        resource.setChangedAt(java.time.LocalDateTime.now());
        resource.setStatusEffectiveFrom(LocalDate.now());

        // Save the resource with notice period details
        resourceRepo.save(resource);

        log.info("Resource {} marked as ON_NOTICE with exit date: {}",
                resource.getFullName(), removalDTO.getNoticePeriodEndDate());

        // Step 3: Trigger attrition immediately if requested
        if (Boolean.TRUE.equals(removalDTO.getTriggerAttritionImmediately())) {
            log.info("Triggering immediate attrition for resource: {}", resource.getResourceId());
            handleAttrition(resource.getResourceId(), removalDTO.getNoticePeriodEndDate(), userId);
            return "Resource removal processed successfully. Resource marked as ON_NOTICE and attrition triggered immediately.";
        } else {
            log.info("Resource marked as ON_NOTICE. Attrition will be triggered automatically by ResourceService when conditions are met.");
            return "Resource removal processed successfully. Resource marked as ON_NOTICE with notice period until " +
                    removalDTO.getNoticePeriodEndDate() + ". Attrition will be triggered automatically.";
        }

    } catch (ProjectExceptionHandler e) {
        log.error("Resource removal failed for resource ID {}: {}", removalDTO.getResourceId(), e.getMessage());
        throw e;
    } catch (Exception e) {
        log.error("Unexpected error during resource removal for resource ID {}: {}",
                removalDTO.getResourceId(), e.getMessage(), e);
        throw new ProjectExceptionHandler(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "RESOURCE_REMOVAL_ERROR",
               "Failed to process resource removal: " + e.getMessage());
        }
    }
}

