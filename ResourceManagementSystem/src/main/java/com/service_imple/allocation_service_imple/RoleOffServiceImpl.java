package com.service_imple.allocation_service_imple;

import com.dto.ApiResponse;
import com.dto.allocation_dto.CloseAllocationDTO;
import com.dto.allocation_dto.RoleOffRequestDTO;
import com.dto.demand_dto.CreateDemandDTO;
import com.entity.allocation_entities.ResourceAllocation;
import com.entity.allocation_entities.RoleOffEvent;
import com.entity.project_entities.Project;
import com.entity.resource_entities.Resource;
import com.entity.skill_entities.DeliveryRoleExpectation;
import com.entity_enums.allocation_enums.AllocationStatus;
import com.entity_enums.allocation_enums.RoleOffType;
import com.entity_enums.demand_enums.DemandCommitment;
import com.entity_enums.demand_enums.DemandStatus;
import com.entity_enums.demand_enums.DemandType;
import com.entity_enums.demand_enums.ReplacementStatus;
import com.entity_enums.allocation_enums.RoleOffReason;
import com.global_exception_handler.ProjectExceptionHandler;
import com.repo.allocation_repo.AllocationRepository;
import com.repo.allocation_repo.RoleOffEventRepository;
import com.repo.project_repo.ProjectRepository;
import com.repo.resource_repo.ResourceRepository;
import com.repo.skill_repo.DeliveryRoleExpectationRepository;
import com.repo.demand_repo.DemandRepository;
import com.service_interface.allocation_service_interface.AllocationService;
import com.service_interface.demand_service_interface.DemandService;
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
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoleOffServiceImpl {
    private final RoleOffEventRepository roleOffRepo;
    private final ProjectRepository projectRepo;
    private final ResourceRepository resourceRepo;
    private final DeliveryRoleExpectationRepository roleRepo;
    private final AllocationRepository allocationRepository;
    private final AllocationService allocationService;
    private final DemandService demandService;
    private final DemandRepository demandRepository;

    // Standardized role-off reasons with descriptions
    private static final Map<RoleOffReason, String> REASON_DESCRIPTIONS = Map.of(
            RoleOffReason.PROJECT_END, "Resource role-off due to project completion or termination",
            RoleOffReason.PERFORMANCE, "Resource role-off due to performance issues",
            RoleOffReason.ATTRITION, "Resource role-off due to employee attrition/resignation",
            RoleOffReason.CLIENT_REQUEST, "Resource role-off due to client request or dissatisfaction"
    );

    @Transactional
    public void roleOff(RoleOffRequestDTO dto, Long userId) {

        // Validate mandatory role-off reason classification using governance method
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

        // 3️⃣ Validate Role
        DeliveryRoleExpectation role = roleRepo.findById(dto.getRoleId())
                .orElseThrow(() -> new ProjectExceptionHandler(
                        HttpStatus.NOT_FOUND,
                        "ROLE_NOT_FOUND",
                        "Role not found"));

        // 4️⃣ Prevent duplicate role-off
        if (roleOffRepo.existsByProject_PmsProjectIdAndResource_ResourceId(
                dto.getProjectId(),
                dto.getResourceId())) {

            throw new ProjectExceptionHandler(
                    HttpStatus.BAD_REQUEST,
                    "ROLE_OFF_ALREADY_EXISTS",
                    "Role-off already initiated for this resource");
        }

        // 5️⃣ Validate RoleOffType
        if (dto.getRoleOffType() == null) {
            throw new ProjectExceptionHandler(
                    HttpStatus.BAD_REQUEST,
                    "ROLE_OFF_TYPE_REQUIRED",
                    "RoleOffType must be provided");
        }

        // 6️⃣ Create RoleOffEvent
        RoleOffEvent event = new RoleOffEvent();

        event.setProject(project);
        event.setResource(resource);
        event.setRole(role);
        event.setRoleOffType(dto.getRoleOffType());
        event.setCreatedBy(userId);

        // ✅ FIX 1 — Set project end date (required for replacement demand)
        if (project.getEndDate() != null) {
            event.setProjectEndDate(project.getEndDate().toLocalDate());
        } else {
            throw new ProjectExceptionHandler(
                    HttpStatus.BAD_REQUEST,
                    "PROJECT_END_DATE_MISSING",
                    "Project end date must be defined before role-off");
        }

        // 7️⃣ Emergency Role-Off
        if (dto.getRoleOffType() == RoleOffType.EMERGENCY) {

            if (dto.getEmergencyReason() == null ||
                    dto.getEmergencyReason().trim().length() < 10) {

                throw new ProjectExceptionHandler(
                        HttpStatus.BAD_REQUEST,
                        "EMERGENCY_REASON_REQUIRED",
                        "Emergency role-off requires a reason with minimum 10 characters");
            }

        event.setProjectEndDate(project.getEndDate().toLocalDate());
        event.setCreatedBy(userId);
        event.setRoleOffReason(dto.getRoleOffReason());
            event.setEmergencyReason(dto.getEmergencyReason());

            // Immediate impact
            event.setRoleOffDate(LocalDate.now());

            // Close active allocation (updates availability ledger)
            closeResourceAllocation(event);
        }

        // 8️⃣ Planned Role-Off
        if (dto.getRoleOffType() == RoleOffType.PLANNED) {

            if (dto.getRoleOffDate() == null) {
                throw new ProjectExceptionHandler(
                        HttpStatus.BAD_REQUEST,
                        "ROLE_OFF_DATE_REQUIRED",
                        "Role-off date required for planned role-off");
            }

            if (dto.getRoleOffDate().isBefore(LocalDate.now())) {
                throw new ProjectExceptionHandler(
                        HttpStatus.BAD_REQUEST,
                        "INVALID_ROLE_OFF_DATE",
                        "Role-off date cannot be in the past");
            }

            event.setRoleOffDate(dto.getRoleOffDate());
        }

        // ✅ FIX 2 — Ensure project end date is after role-off date
        if (event.getProjectEndDate().isBefore(event.getRoleOffDate())) {
            throw new ProjectExceptionHandler(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_PROJECT_TIMELINE",
                    "Project end date cannot be before role-off date");
        }

        // 9️⃣ Replacement Logic
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

        // 🔟 Save event
        roleOffRepo.save(event);
    }

    private void closeResourceAllocation(RoleOffEvent event) {

        Optional<ResourceAllocation> allocationOpt =
                allocationRepository
                        .findByProject_PmsProjectIdAndResource_ResourceIdAndAllocationStatus(
                                event.getProject().getPmsProjectId(),
                                event.getResource().getResourceId(),
                                AllocationStatus.ACTIVE
                        );

        if (allocationOpt.isEmpty()) {
            throw new ProjectExceptionHandler(
                    HttpStatus.BAD_REQUEST,
                    "ACTIVE_ALLOCATION_NOT_FOUND",
                    "No active allocation found");
        }

        ResourceAllocation allocation = allocationOpt.get();

        CloseAllocationDTO closeDTO = new CloseAllocationDTO();
        closeDTO.setClosureDate(event.getRoleOffDate());

        allocationService.closeAllocation(allocation.getAllocationId(), closeDTO);
    }

    private void createReplacementDemand(RoleOffEvent event, Long userId) {

        CreateDemandDTO dto = new CreateDemandDTO();

        dto.setProjectId(event.getProject().getId());
        dto.setDeliveryRole(event.getRole().getId());

        dto.setDemandName("Replacement for " + event.getResource().getFullName());

        dto.setDemandType(DemandType.REPLACEMENT);

        dto.setDemandStartDate(event.getRoleOffDate().plusDays(1));

        dto.setDemandEndDate(event.getProjectEndDate());

        dto.setAllocationPercentage(100);

        dto.setDemandStatus(DemandStatus.REQUESTED);

        dto.setDemandPriority(event.getProject().getPriorityLevel());

        dto.setDemandCommitment(DemandCommitment.CONFIRMED);

        dto.setResourcesRequired(1);

        Long exp = event.getResource().getExperiance();

        dto.setMinExp(exp != null ? (double) exp.intValue() : 0.0);

        dto.setOutgoingResourceId(event.getResource().getResourceId());

        ResponseEntity<ApiResponse<?>> response =
                demandService.createDemand(dto, userId);

        if (!response.getStatusCode().is2xxSuccessful()) {

            throw new ProjectExceptionHandler(
                    HttpStatus.BAD_REQUEST,
                    "REPLACEMENT_DEMAND_FAILED",
                    "Replacement demand creation failed");
        }
    }
    @Transactional
    public void manualReplacement(UUID roleOffEventId, Long userId) {

        RoleOffEvent event = roleOffRepo.findById(roleOffEventId)
                .orElseThrow(() -> new RuntimeException("RoleOff not found"));

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
        validateBusinessRules(event.getRoleOffReason());
    }

    /**
     * Gets statistics for role-off reasons reporting
     */
    public Map<RoleOffReason, Long> getRoleOffReasonStatistics() {
        return roleOffRepo.findAll().stream()
                .filter(event -> event.getRoleOffReason() != null)
                .collect(Collectors.groupingBy(
                        RoleOffEvent::getRoleOffReason,
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
}
