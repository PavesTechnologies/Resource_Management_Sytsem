package com.service_imple.allocation_service_imple;

import com.dto.ApiResponse;
import com.dto.allocation_dto.RoleOffRequestDTO;
import com.dto.demand_dto.CreateDemandDTO;
import com.entity.allocation_entities.RoleOffEvent;
import com.entity.project_entities.Project;
import com.entity.resource_entities.Resource;
import com.entity.skill_entities.DeliveryRoleExpectation;
import com.entity_enums.demand_enums.DemandCommitment;
import com.entity_enums.demand_enums.DemandStatus;
import com.entity_enums.demand_enums.DemandType;
import com.entity_enums.demand_enums.ReplacementStatus;
import com.entity_enums.allocation_enums.RoleOffReason;
import com.global_exception_handler.ProjectExceptionHandler;
import com.repo.allocation_repo.RoleOffEventRepository;
import com.repo.project_repo.ProjectRepository;
import com.repo.resource_repo.ResourceRepository;
import com.repo.skill_repo.DeliveryRoleExpectationRepository;
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

@Service
@RequiredArgsConstructor
public class RoleOffServiceImpl {
    private final RoleOffEventRepository roleOffRepo;
    private final ProjectRepository projectRepo;
    private final ResourceRepository resourceRepo;
    private final DeliveryRoleExpectationRepository roleRepo;
    private final DemandService demandService;

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

        Project project = projectRepo.findById(dto.getProjectId())
                .orElseThrow(() -> new ProjectExceptionHandler(
                        HttpStatus.NOT_FOUND,
                        "PROJECT_NOT_FOUND",
                        "Project not found"
                ));

        Resource resource = resourceRepo.findById(dto.getResourceId())
                .orElseThrow(() -> new ProjectExceptionHandler(
                        HttpStatus.NOT_FOUND,
                        "RESOURCE_NOT_FOUND",
                        "Resource not found"
                ));

        DeliveryRoleExpectation role = roleRepo.findById(dto.getRoleId())
                .orElseThrow(() -> new ProjectExceptionHandler(
                        HttpStatus.NOT_FOUND,
                        "ROLE_NOT_FOUND",
                        "Role not found"
                ));

        RoleOffEvent event = new RoleOffEvent();
        event.setProject(project);
        event.setResource(resource);
        event.setRole(role);
        event.setRoleOffDate(dto.getRoleOffDate());
        if (project.getEndDate() == null) {
            throw new ProjectExceptionHandler(
                    HttpStatus.BAD_REQUEST,
                    "PROJECT_END_DATE_MISSING",
                    "Project end date must be defined before role off"
            );
        }

        event.setProjectEndDate(project.getEndDate().toLocalDate());
        event.setCreatedBy(userId);
        event.setRoleOffReason(dto.getRoleOffReason());

        if (Boolean.TRUE.equals(dto.getAutoReplacementRequired())) {

            if (event.getReplacementStatus() == ReplacementStatus.AUTO_CREATED) {
                throw new ProjectExceptionHandler(
                        HttpStatus.BAD_REQUEST,
                        "REPLACEMENT_ALREADY_CREATED",
                        "Replacement demand already exists for this role-off event"
                );
            }

            createReplacementDemand(event, userId);
            event.setReplacementStatus(ReplacementStatus.AUTO_CREATED);
        } else {

            if (dto.getSkipReason() == null || dto.getSkipReason().isBlank()) {
                throw new ProjectExceptionHandler(
                        HttpStatus.BAD_REQUEST,
                        "SKIP_REASON_REQUIRED",
                        "Skip reason is mandatory when auto replacement is false"
                );
            }

            event.setReplacementStatus(ReplacementStatus.SKIPPED);
            event.setSkipReason(dto.getSkipReason());
        }

        roleOffRepo.save(event);
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
        dto.setMinExp(event.getResource().getExperiance() != null ? event.getResource().getExperiance().intValue() : 0.0);
        dto.setOutgoingResourceId(event.getResource().getResourceId());

        ResponseEntity<ApiResponse<?>> response = demandService.createDemand(dto, userId);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new ProjectExceptionHandler(
                    HttpStatus.BAD_REQUEST,
                    "REPLACEMENT_DEMAND_FAILED",
                    "Replacement demand creation failed: " + response.getBody().getMessage()
            );
        }
    }
    @Transactional
    public void manualReplacement(Long roleOffEventId, Long userId) {

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
}
