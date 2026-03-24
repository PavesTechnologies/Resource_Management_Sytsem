package com.service_imple.roleoff_service_impl;

import com.dto.ApiResponse;
import com.dto.UserDTO;
import com.dto.allocation_dto.CloseAllocationDTO;
import com.dto.roleoff_dto.RoleOffRequestDTO;
import com.dto.roleoff_dto.ResourcesDTO;
import com.dto.demand_dto.CreateDemandDTO;
import com.entity.allocation_entities.ResourceAllocation;
import com.entity.allocation_entities.RoleOffEvent;
import com.entity.project_entities.Project;
import com.entity.resource_entities.Resource;
import com.entity.skill_entities.DeliveryRoleExpectation;
import com.entity_enums.allocation_enums.AllocationStatus;
import com.entity_enums.allocation_enums.RoleOffStatus;
import com.entity_enums.allocation_enums.RoleOffType;
import com.entity_enums.demand_enums.DemandCommitment;
import com.entity_enums.demand_enums.DemandStatus;
import com.entity_enums.demand_enums.DemandType;
import com.entity_enums.demand_enums.ReplacementStatus;
import com.global_exception_handler.AllocationExceptionHandler;
import com.entity_enums.allocation_enums.RoleOffReason;
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
import com.service_imple.skill_service_impl.ResourceSkillUsageService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
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
import java.time.chrono.ChronoLocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoleOffServiceImpl implements RoleOffService {
    private final RoleOffEventRepository roleOffRepo;
    private final ProjectRepository projectRepo;
    private final ResourceRepository resourceRepo;
    private final DeliveryRoleExpectationRepository roleRepository;
    private final AllocationRepository allocationRepository;
    private final ResourceSkillRepository resourceSkillRepository;
    private final ResourceSubSkillRepository resourceSubSkillRepository;
    private final AllocationService allocationService;
    private final DemandService demandService;
    private final DemandRepository demandRepository;
    private final ResourceSkillUsageService resourceSkillUsageService;

    // Standardized role-off reasons with descriptions
    private static final Map<RoleOffReason, String> REASON_DESCRIPTIONS = Map.of(
            RoleOffReason.PROJECT_END, "Resource role-off due to project completion or termination",
            RoleOffReason.PERFORMANCE, "Resource role-off due to performance issues",
            RoleOffReason.ATTRITION, "Resource role-off due to employee attrition/resignation",
            RoleOffReason.CLIENT_REQUEST, "Resource role-off due to client request or dissatisfaction"
    );

    @Transactional
    public ResponseEntity<?> roleOff(com.dto.allocation_dto.RoleOffRequestDTO dto, Long userId) {

        // Validate mandatory role-off reason classification
        validateRoleOffReason(dto.getRoleOffReason());

        // 1️⃣ Validate Project
        Project project = projectRepo.findById(dto.getProjectId())
                .orElseThrow(() -> new ProjectExceptionHandler(
                        HttpStatus.NOT_FOUND,
                        "PROJECT_NOT_FOUND",
                        "Project not found"));

        // 2️⃣ Validate Resource
        Resource resource = resourceRepo.findById(dto.getResourceId())
                .orElseThrow(() -> new ProjectExceptionHandler(
                        HttpStatus.NOT_FOUND,
                        "RESOURCE_NOT_FOUND",
                        "Resource not found"));

        // 3️⃣ Find active allocation
        ResourceAllocation allocation;

        if (dto.getAllocationId() != null) {

            allocation = allocationRepository.findById(dto.getAllocationId())
                    .orElseThrow(() -> new ProjectExceptionHandler(
                            HttpStatus.NOT_FOUND,
                            "ALLOCATION_NOT_FOUND",
                            "Allocation not found"));

            if (!allocation.getProject().getPmsProjectId().equals(project.getPmsProjectId()) ||
                    !allocation.getResource().getResourceId().equals(resource.getResourceId())) {
                throw new ProjectExceptionHandler(
                        HttpStatus.BAD_REQUEST,
                        "ALLOCATION_MISMATCH",
                        "Allocation mismatch");
            }

            if (!AllocationStatus.ACTIVE.equals(allocation.getAllocationStatus())) {
                throw new ProjectExceptionHandler(
                        HttpStatus.BAD_REQUEST,
                        "ALLOCATION_NOT_ACTIVE",
                        "Allocation is not active");
            }

        } else {

            List<ResourceAllocation> allocations =
                    allocationRepository
                            .findAllByProject_PmsProjectIdAndResource_ResourceIdAndAllocationStatus(
                                    project.getPmsProjectId(),
                                    resource.getResourceId(),
                                    AllocationStatus.ACTIVE
                            );

            if (allocations.isEmpty()) {
                throw new ProjectExceptionHandler(
                        HttpStatus.BAD_REQUEST,
                        "ACTIVE_ALLOCATION_NOT_FOUND",
                        "No active allocation found");
            }

            if (allocations.size() > 1) {
                throw new ProjectExceptionHandler(
                        HttpStatus.BAD_REQUEST,
                        "MULTIPLE_ACTIVE_ALLOCATIONS_FOUND",
                        "Multiple active allocations found. Provide allocationId.");
            }

            allocation = allocations.get(0);
        }

        // 4️⃣ Handle existing role-off records
        RoleOffEvent existingRoleOff = roleOffRepo.findByAllocation_AllocationId(allocation.getAllocationId());

        if (existingRoleOff != null) {
            // If existing role-off is REJECTED, delete it and allow new one
            if (existingRoleOff.getRoleOffStatus() == RoleOffStatus.REJECTED) {
                // Log the deletion for audit purposes
                logRoleOffDeletion(existingRoleOff, "Creating new role-off request for rejected resource");

                // Delete the rejected record
                roleOffRepo.delete(existingRoleOff);
            }
            // If existing role-off is in any other status, prevent duplicate
            else {
                throw new ProjectExceptionHandler(
                        HttpStatus.BAD_REQUEST,
                        "ROLE_OFF_ALREADY_EXISTS",
                        "Role-off already initiated for this allocation with status: " + existingRoleOff.getRoleOffStatus());
            }
        }

        // 5️⃣ IMPACT PREVIEW MODE (if not confirmed)
        if (dto.getConfirmed() == null || !dto.getConfirmed()) {
            return generateImpactPreview(dto, userId, resource, project, allocation);
        }

        // 6️⃣ PROCESS ROLE-OFF (confirmed)
        processRoleOff(dto, userId, resource, project, allocation);

        return ResponseEntity.ok(new ApiResponse<>(true, "Processed successfully", null));
    }

    private void closeResourceAllocation(RoleOffEvent event) {

        ResourceAllocation allocation = event.getAllocation();

        CloseAllocationDTO closeDTO = new CloseAllocationDTO();
        closeDTO.setClosureDate(event.getEffectiveRoleOffDate());

        allocationService.closeAllocation(allocation.getAllocationId(), closeDTO);

        // Update skill lastUsedDate for role-off
        try {
            resourceSkillUsageService.updateResourceSkillLastUsedOnRoleOff(
                event.getResource(),
                event.getProject(),
                event.getEffectiveRoleOffDate()
            );
        } catch (Exception e) {
            // Log error but don't fail the role-off process
            System.err.println("Failed to update skill lastUsedDate for resource " +
                event.getResource().getResourceId() + ": " + e.getMessage());
        }
    }

    private void createReplacementDemand(RoleOffEvent event, Long userId) {

        CreateDemandDTO dto = new CreateDemandDTO();

        dto.setProjectId(event.getProject().getPmsProjectId()); // ✅ Use pmsProjectId instead of id
        
        // Try to get role from allocation's demand first (demand-based allocation)
        if (event.getAllocation() != null && event.getAllocation().getDemand() != null && event.getAllocation().getDemand().getRole() != null) {
            dto.setDeliveryRole(event.getAllocation().getDemand().getRole().getId());
        }
        // Fall back to replacement role from event (project-based allocation)
        else if (event.getRole() != null) {
            dto.setDeliveryRole(event.getRole().getId());
        }
        // No role available - skip replacement demand creation
        else {
            return;
        }

        dto.setDemandName("Replacement for " + event.getResource().getFullName());

        dto.setDemandType(DemandType.REPLACEMENT);

        dto.setDemandStartDate(event.getEffectiveRoleOffDate().plusDays(1));

        dto.setDemandEndDate(event.getProject().getEndDate().toLocalDate());

        dto.setAllocationPercentage(event.getAllocation().getAllocationPercentage());

        dto.setDemandStatus(DemandStatus.REQUESTED);

        dto.setDemandPriority(event.getProject().getPriorityLevel());

        dto.setDemandCommitment(DemandCommitment.CONFIRMED);

        dto.setResourcesRequired(1);

        Long exp = event.getResource().getExperiance();

        dto.setMinExp(exp != null ? (double) exp.intValue() : 0.0);
//        dto.setMaxExp(exp != null ? (double) (exp.intValue() + 5) : 5.0); // ✅ Add maxExp

        // ✅ Add missing required fields
        dto.setDeliveryModel(com.entity_enums.centralised_enums.DeliveryModel.ONSITE); // Default to ONSITE
        dto.setDemandJustification("Emergency replacement for " + event.getResource().getFullName() + 
                                  " due to " + event.getRoleOffReasonEnum());
        dto.setRequiresAdditionalApproval(false);

        dto.setOutgoingResourceId(event.getResource().getResourceId());

        ResponseEntity<ApiResponse<?>> response =
                demandService.createDemand(dto, userId);

        if (!response.getStatusCode().is2xxSuccessful()) {
            // Log the actual error details for debugging
            String errorMessage = "Replacement demand creation failed";
            if (response.getBody() != null && response.getBody().getMessage() != null) {
                errorMessage = "Replacement demand creation failed: " + response.getBody().getMessage();
            }
            
            throw new ProjectExceptionHandler(
                    HttpStatus.BAD_REQUEST,
                    "REPLACEMENT_DEMAND_FAILED",
                    errorMessage);
        }
    }
    @Transactional
    public void manualReplacement(UUID roleOffEventId, Long userId) {

        RoleOffEvent event = roleOffRepo.findById(roleOffEventId)
                .orElseThrow(() -> new RuntimeException("RoleOff event with ID " + roleOffEventId + " not found. The event may have been deleted or the ID is incorrect."));

        if (event.getReplacementStatus() == ReplacementStatus.AUTO_CREATED ||
                event.getReplacementStatus() == ReplacementStatus.MANUAL_CREATED_LATER) {

            throw new ProjectExceptionHandler(
                    HttpStatus.BAD_REQUEST,
                    "REPLACEMENT_ALREADY_EXISTS",
                    "Replacement demand already created for this role-off event"
            );
        }

        createReplacementDemand(event, userId);

        event.setReplacementStatus(ReplacementStatus.MANUAL_CREATED_LATER);
        roleOffRepo.save(event);
    }

    @Override
    public ResponseEntity<?> roleOffByRM(RoleOffRequestDTO roleOff, UserDTO userDTO) {

        Project project = projectRepo.findById(roleOff.getProjectId())
                .orElseThrow(() -> ProjectExceptionHandler.badRequest("Project not found!"));

        if (roleOff.getResourceIds() == null || roleOff.getResourceIds().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "At least one resource is required", null));
        }

        if (roleOff.getAllocationId() == null || roleOff.getAllocationId().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Allocation Ids required", null));
        }

        LocalDate today = LocalDate.now();
        if (roleOff.getEffectiveRoleOffDate().isBefore(today)) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Effective Role Off Date should be today or future", null));
        }

        List<RoleOffEvent> successEvents = new ArrayList<>();
        List<Map<String, Object>> failedEvents = new ArrayList<>();

//        List<Resource> resources = resourceRepo.findAllById(roleOff.getResourceIds());
        List<ResourceAllocation> allocations = allocationRepository.findAllById(roleOff.getAllocationId());

//        Map<Long, Resource> resourceMap = resources.stream()
//                .collect(Collectors.toMap(Resource::getResourceId, r -> r));

        Map<UUID, ResourceAllocation> allocationMap = allocations.stream()
                .collect(Collectors.toMap(ResourceAllocation::getAllocationId, a -> a));

        for (UUID allocationId : roleOff.getAllocationId()) {

            ResourceAllocation allocation = allocationMap.get(allocationId);

            if (allocation == null) {
                failedEvents.add(Map.of(
                        "allocationId", allocationId,
                        "reason", "Allocation not found"
                ));
                continue;
            }

            Resource resource = allocation.getResource();

            if (allocation.getAllocationStatus() != AllocationStatus.ACTIVE) {
                failedEvents.add(Map.of(
                        "resourceId", resource.getResourceId(),
                        "reason", "Allocation not active"
                ));
                continue;
            }

            if (!allocation.getProject().getPmsProjectId().equals(project.getPmsProjectId())) {
                failedEvents.add(Map.of(
                        "resourceId", resource.getResourceId(),
                        "reason", "Project mismatch"
                ));
                continue;
            }

            try {

                // Handle existing role-off records
                RoleOffEvent existingRoleOff = roleOffRepo.findByAllocation_AllocationId(allocation.getAllocationId());

                if (existingRoleOff != null) {
                    // If existing role-off is REJECTED, delete it and allow new one
                    if (existingRoleOff.getRoleOffStatus() == RoleOffStatus.REJECTED) {
                        // Log the deletion for audit purposes
                        logRoleOffDeletion(existingRoleOff, "Creating new role-off request for rejected resource");

                        // Delete the rejected record
                        roleOffRepo.delete(existingRoleOff);
                    }
                    // If existing role-off is in any other status, skip this allocation
                    else {
                        failedEvents.add(Map.of(
                                "allocationId", allocationId,
                                "reason", "Role-off already exists with status: " + existingRoleOff.getRoleOffStatus()
                        ));
                        continue;
                    }
                }

                RoleOffEvent event = new RoleOffEvent();
                event.setProject(project);
                event.setAllocation(allocation);
                event.setResource(resource);
                event.setRoleOffType(roleOff.getRoleOffType());
                event.setEffectiveRoleOffDate(roleOff.getEffectiveRoleOffDate());
                event.setRoleOffStatus(RoleOffStatus.PENDING);
                event.setRoleOffReason(roleOff.getRoleOffReason());
                event.setCreatedAt(LocalDate.now());
                event.setCreatedBy(userDTO.getId());
                event.setRoleInitiatedBy(
                        userDTO.getRoles().contains("RESOURCE-MANAGER")
                                ? "RESOURCE-MANAGER"
                                : "Unknown"
                );

                successEvents.add(event);

            } catch (Exception ex) {

                failedEvents.add(Map.of(
                        "resourceId", resource.getResourceId(),
                        "reason", ex.getMessage()
                ));
            }
        }

        List<RoleOffEvent> savedEvents = roleOffRepo.saveAll(successEvents);

        Map<String, Object> result = new HashMap<>();
        result.put("successCount", savedEvents.size());
        result.put("failedCount", failedEvents.size());
        result.put("success", savedEvents);
        result.put("failed", failedEvents);

        String message;

        if (!savedEvents.isEmpty() && !failedEvents.isEmpty()) {
            message = savedEvents.size() + " Role Off created, " + failedEvents.size() + " failed";
        } else if (!savedEvents.isEmpty()) {
            message = "All Role Off requests created successfully";
        } else {
            message = "All Role Off requests failed";
        }

        return ResponseEntity.ok().body(new ApiResponse<>(true, message, result));
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

        Map<Long, String> impactLevels = new HashMap<>();

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

                impactLevels.put(resourceId, impactLevel);
            }

        } catch (Exception e) {
            // Fallback to LOW impact for all resources if calculation fails
            for (Long resourceId : resourceIds) {
                impactLevels.put(resourceId, "LOW");
            }
        }

        return impactLevels;
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
            Resource resource = resourceRepo.findById(resourceId).get();
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
    public String validateDeliveryImpact(com.dto.allocation_dto.RoleOffRequestDTO dto) {
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
    public void logRoleOffDecision(com.dto.allocation_dto.RoleOffRequestDTO dto, String warning, boolean confirmed, Long userId) {
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
                resourceSkillRepository.findSkillsByResourceIds(resourceIds)
        );

        Map<Long, List<String>> subSkillMap = mapToGroupedList(
                resourceSubSkillRepository.findSubSkillsByResourceIds(resourceIds)
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

    @Override
    public ResponseEntity<?> getRoleOffKPI(Long projectId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Role-off KPI retrived successfully!", roleOffRepo.getProjectKPI(projectId)));
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

        // Get the role from allocation or replacement role
        DeliveryRoleExpectation roleToSet = null;

        // First try to get role from the allocation's demand (demand-based allocation)
        if (dto.getAllocationId() != null) {
            ResourceAllocation tempAllocation = allocationRepository.findById(dto.getAllocationId()).orElse(null);
            if (tempAllocation != null && tempAllocation.getDemand() != null) {
                roleToSet = tempAllocation.getDemand().getRole();
            }
        }

        // If auto replacement is required, we must have a role
        if (Boolean.TRUE.equals(dto.getAutoReplacementRequired())) {
            // If no role from demand, use replacement role (must be provided when auto replacement is true)
            if (roleToSet == null) {
                if (dto.getReplacementRoleId() != null) {
                    roleToSet = roleRepository.findById(dto.getReplacementRoleId())
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
            // Auto replacement is false - role is optional, but use it if provided
            if (roleToSet == null && dto.getReplacementRoleId() != null) {
                roleToSet = roleRepository.findById(dto.getReplacementRoleId())
                        .orElseThrow(() -> new ProjectExceptionHandler(
                                HttpStatus.NOT_FOUND,
                                "REPLACEMENT_ROLE_NOT_FOUND",
                                "Replacement role not found"));
            }
        }

        // Create RoleOffEvent
        RoleOffEvent event = new RoleOffEvent();

        event.setProject(project);
        event.setResource(resource);
        event.setRole(roleToSet); // Always set the role (can be null if neither demand nor replacement role exists)
        event.setRoleOffType(dto.getRoleOffType());
        event.setRoleInitiatedBy("PROJECT-MANAGER");
        event.setCreatedBy(userId);
        event.setAllocation(allocation);

        // COMMON FIELDS
        event.setRoleOffReasonEnum(dto.getRoleOffReason());
        event.setEffectiveRoleOffDate(dto.getEffectiveRoleOffDate());

        // =========================================================
        // 🔥 EMERGENCY ROLE-OFF (IMMEDIATE EXECUTION)
        // =========================================================
        if (dto.getRoleOffType() == RoleOffType.EMERGENCY) {

            event.setRoleOffReason(dto.getEmergencyReason());
            event.setRoleOffStatus(RoleOffStatus.APPROVED);
//            event.setApprovedBy("SYSTEM");

            roleOffRepo.save(event); // ✅ FIRST SAVE

            // 🔥 REPLACEMENT LOGIC
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

            roleOffRepo.save(event); // ✅ UPDATE AFTER DEMAND

            // 🔥 immediate execution
            closeResourceAllocation(event);

            event.setRoleOffStatus(RoleOffStatus.FULFILLED);
            roleOffRepo.save(event);

            return;
        }

        // =========================================================
        // 🔥 PLANNED ROLE-OFF (APPROVAL FLOW)
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

            event.setRoleOffReason(dto.getRoleOffReason().name());
            event.setRoleOffStatus(RoleOffStatus.PENDING);

            roleOffRepo.save(event); // ✅ SAVE FIRST

            // 🔥 REPLACEMENT LOGIC
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

            roleOffRepo.save(event); // ✅ UPDATE AFTER DEMAND
        }

        // FINAL SAVE
        roleOffRepo.save(event);
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

        return ResponseEntity.ok("Approved by RM → Waiting for DL");
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
        return ResponseEntity.ok("Rejected by RM");
    }

    // ========== SEPARATE FULFILL/REJECT METHODS FOR DELIVERY MANAGER ==========

    @Override
    @Transactional
    public ResponseEntity<?> dlFulfill(UUID id, UserDTO userDTO) {
        RoleOffEvent event = roleOffRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("RoleOff event with ID " + id + " not found. The event may have been deleted or the ID is incorrect."));

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

        // 🔥 IF DATE PASSED → EXECUTE IMMEDIATELY
        if (!event.getEffectiveRoleOffDate().isAfter(LocalDate.now())) {
            closeResourceAllocation(event);
            roleOffRepo.save(event);
            return ResponseEntity.ok("Executed immediately");
        }

        // Wait for scheduler to execute on effective date
        roleOffRepo.save(event);
        return ResponseEntity.ok("Fulfilled → waiting for execution on effective date");
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
        return ResponseEntity.ok("Rejected by DL");
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
}
