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
import com.global_exception_handler.ProjectExceptionHandler;
import com.repo.allocation_repo.AllocationRepository;
import com.repo.allocation_repo.RoleOffEventRepository;
import com.repo.project_repo.ProjectRepository;
import com.repo.resource_repo.ResourceRepository;
import com.repo.skill_repo.DeliveryRoleExpectationRepository;
import com.service_interface.allocation_service_interface.AllocationService;
import com.service_interface.demand_service_interface.DemandService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
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

    @Transactional
    public void roleOff(RoleOffRequestDTO dto, Long userId) {

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
}
