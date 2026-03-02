package com.service_imple.allocation_service_imple;

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
import com.global_exception_handler.ProjectExceptionHandler;
import com.repo.allocation_repo.RoleOffEventRepository;
import com.repo.project_repo.ProjectRepository;
import com.repo.resource_repo.ResourceRepository;
import com.repo.skill_repo.DeliveryRoleExpectationRepository;
import com.service_interface.demand_service_interface.DemandService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoleOffServiceImpl {
    private final RoleOffEventRepository roleOffRepo;
    private final ProjectRepository projectRepo;
    private final ResourceRepository resourceRepo;
    private final DeliveryRoleExpectationRepository roleRepo;
    private final DemandService demandService;

    @Transactional
    public void roleOff(RoleOffRequestDTO dto, Long userId) {

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
        event.setProjectEndDate(project.getEndDate().toLocalDate());
        event.setCreatedBy(userId);

        if (Boolean.TRUE.equals(dto.getAutoReplacementRequired())) {

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

        dto.setProjectId(event.getProject().getPmsProjectId());
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

        demandService.createDemand(dto, userId);
    }
    @Transactional
    public void manualReplacement(Long roleOffEventId, Long userId) {

        RoleOffEvent event = roleOffRepo.findById(roleOffEventId)
                .orElseThrow(() -> new RuntimeException("RoleOff not found"));

        if (event.getReplacementStatus() == ReplacementStatus.AUTO_CREATED) {
            throw new RuntimeException("Replacement already created.");
        }

        createReplacementDemand(event, userId);

        event.setReplacementStatus(ReplacementStatus.MANUAL_CREATED_LATER);
        roleOffRepo.save(event);
    }
}
