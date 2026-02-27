package com.service_imple.demand_service_impl;

import com.dto.ApiResponse;
import com.dto.demand_dto.CreateDemandDTO;
import com.dto.demand_dto.UpdateDemandDTO;
import com.entity.demand_entities.Demand;
import com.entity.project_entities.Project;
import com.entity.project_entities.ProjectCompliance;
import com.entity.resource_entities.Resource;
import com.entity.skill_entities.DeliveryRoleExpectation;
import com.entity_enums.centralised_enums.PriorityLevel;
import com.entity_enums.client_enums.RequirementType;
//import com.entity_enums.skill_enums.DemandStatus;
import com.entity_enums.demand_enums.DemandCommitment;
import com.entity_enums.demand_enums.DemandType;
import com.global_exception_handler.ProjectExceptionHandler;
import com.repo.DemandRepository;
import com.repo.project_repo.ProjectComplianceRepo;
import com.repo.project_repo.ProjectRepository;
import com.repo.resource_repo.ResourceRepository;
import com.repo.skill_repo.DeliveryRoleExpectationRepository;
import com.service_imple.project_service_impl.ProjectDemandValidationService;
import com.service_interface.demand_service_interface.DemandService;
import com.service_interface.skill_service_interface.DeliveryRoleExpectationService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class DemandServiceImpl implements DemandService {

    @Autowired
    private DemandRepository demandRepository;

    @Autowired
    private ProjectDemandValidationService projectDemandValidationService;

    @Autowired
    private ProjectComplianceRepo projectComplianceRepo;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private DeliveryRoleExpectationRepository roleRepository;

    @Override
    public ResponseEntity<ApiResponse<?>> createDemand(CreateDemandDTO dto, Long userId) {
        try {

            // 🔐 Validate project eligibility
//            projectDemandValidationService.validateProjectForStaffing(dto.getProjectId());
            // Fetch Project
            Project project = projectRepository.findById(dto.getProjectId())
                    .orElseThrow(() -> new ProjectExceptionHandler(
                            HttpStatus.NOT_FOUND,
                            "PROJECT_NOT_FOUND",
                            "Project not found"
                    ));
            if (dto.getDemandStartDate().isBefore(project.getStartDate()) || dto.getDemandEndDate().isAfter(project.getEndDate())) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Demand date range is not in between project date range"));
            }

            // Fetch Role
            DeliveryRoleExpectation role = roleRepository.findById(dto.getRoleId())
                    .orElseThrow(() -> new ProjectExceptionHandler(
                            HttpStatus.NOT_FOUND,
                            "ROLE_NOT_FOUND",
                            "Role not found"
                    ));

            // Create Entity
            Demand demand = new Demand();
            demand.setProject(project);
            demand.setRole(role);
            demand.setDemandName(dto.getDemandName());
            demand.setDemandType(dto.getDemandType());
            demand.setDemandStartDate(dto.getDemandStartDate());
            demand.setDemandEndDate(dto.getDemandEndDate());
            demand.setAllocationPercentage(dto.getAllocationPercentage());
            demand.setDeliveryModel(dto.getDeliveryModel());
            demand.setDemandStatus(dto.getDemandStatus());
            demand.setDemandJustification(dto.getDemandJustification());
            demand.setDemandPriority(dto.getDemandPriority());
            demand.setMinExp(dto.getMinExp());
            demand.setResourcesRequired(dto.getResourcesRequired());
            demand.setCreatedBy(userId);
            demand.setDemandCommitment(dto.getDemandCommitment());
            demand.setRequiresAdditionalApproval(dto.getRequiresAdditionalApproval());
            demand.setCreatedAt(LocalDateTime.now());

            // Handle outgoing resource (Replacement case)
            if ( dto.getDemandType() == DemandType.REPLACEMENT && dto.getOutgoingResourceId() != null) {
                Resource resource = resourceRepository.findById(dto.getOutgoingResourceId())
                        .orElseThrow(() -> new ProjectExceptionHandler(
                                HttpStatus.NOT_FOUND,
                                "RESOURCE_NOT_FOUND",
                                "Outgoing resource not found"
                        ));
                demand.setOutgoingResource(resource);
            }

            // 🔥 Validate demand type rules
            validateDemandTypeRules(demand);

            // 🔥 Apply business rules
            applyDemandTypeRules(demand);

            // 🔹 Attach compliance requirements
            attachMandatoryCompliances(demand);

            Demand saved = demandRepository.save(demand);

            return new ResponseEntity<>(
                    ApiResponse.success("Demand created successfully", saved.getDemandId()),
                    HttpStatus.CREATED
            );

        } catch (ProjectExceptionHandler e) {
            return new ResponseEntity<>(
                    ApiResponse.error(e.getMessage()),
                    e.getStatus()
            );
        } catch (Exception e) {
            return new ResponseEntity<>(
                    ApiResponse.error("Failed to create demand: " + e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    @Transactional
    @Override
    public ResponseEntity<ApiResponse<?>> updateDemand(UpdateDemandDTO dto) {

        if (dto.getDemandId() == null) {
            throw new ProjectExceptionHandler(
                    HttpStatus.BAD_REQUEST,
                    "DEMAND_ID_REQUIRED",
                    "Demand ID is required"
            );
        }

        Demand existing = demandRepository.findById(dto.getDemandId())
                .orElseThrow(() -> new ProjectExceptionHandler(
                        HttpStatus.NOT_FOUND,
                        "DEMAND_NOT_FOUND",
                        "Demand not found"
                ));

        // Date validation
        if (dto.getDemandStartDate() != null &&
                dto.getDemandEndDate() != null &&
                dto.getDemandEndDate().isBefore(dto.getDemandStartDate())) {

            throw new ProjectExceptionHandler(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_DATE_RANGE",
                    "Demand end date cannot be before start date"
            );
        }

        // Safe updates
        if (dto.getDemandType() != null)
            existing.setDemandType(dto.getDemandType());

        if (dto.getDemandStatus() != null)
            existing.setDemandStatus(dto.getDemandStatus());

        if (dto.getDemandJustification() != null)
            existing.setDemandJustification(dto.getDemandJustification());

        if (dto.getDemandStartDate() != null)
            existing.setDemandStartDate(dto.getDemandStartDate());

        if (dto.getDemandEndDate() != null)
            existing.setDemandEndDate(dto.getDemandEndDate());

        if (dto.getAllocationPercentage() != null)
            existing.setAllocationPercentage(dto.getAllocationPercentage());

        if (dto.getDeliveryModel() != null)
            existing.setDeliveryModel(dto.getDeliveryModel());

        if (dto.getDemandPriority() != null)
            existing.setDemandPriority(dto.getDemandPriority());

        if (dto.getDemandCommitment() != null)
            existing.setDemandCommitment(dto.getDemandCommitment());

        if (dto.getRequiresAdditionalApproval() != null)
            existing.setRequiresAdditionalApproval(dto.getRequiresAdditionalApproval());

        if (dto.getOutgoingResourceId() != null) {
            if (dto.getDemandType() == DemandType.REPLACEMENT && dto.getOutgoingResourceId() != 0) {
                Resource resource = resourceRepository.findById(dto.getOutgoingResourceId())
                        .orElseThrow(() -> new ProjectExceptionHandler(
                                HttpStatus.NOT_FOUND,
                                "RESOURCE_NOT_FOUND",
                                "Outgoing resource not found"
                        ));
                existing.setOutgoingResource(resource);
            } else if (dto.getOutgoingResourceId() == 0) {
                // Explicitly set to null when ID is 0
                existing.setOutgoingResource(null);
            }
        }

        // Re-validate rules
        validateDemandTypeRules(existing);
        applyDemandTypeRules(existing);

        demandRepository.save(existing);

        return ResponseEntity.ok(
                ApiResponse.success("Demand updated successfully", existing.getDemandId())
        );
    }


    @Override
    public ResponseEntity<ApiResponse<?>> getDemandByProjectId(Long projectId) {
        try {

            if (projectId == null) {
                throw new ProjectExceptionHandler(
                        HttpStatus.BAD_REQUEST,
                        "PROJECT_ID_REQUIRED",
                        "Project ID is required"
                );
            }

            projectDemandValidationService.validateProjectForStaffing(projectId);

            List<Demand> demands = demandRepository.findByProject_PmsProjectId(projectId);

            return new ResponseEntity<>(
                    ApiResponse.success("Demands retrieved successfully", demands),
                    HttpStatus.OK
            );

        } catch (ProjectExceptionHandler e) {
            return new ResponseEntity<>(
                    ApiResponse.error(e.getMessage()),
                    e.getStatus()
            );
        } catch (Exception e) {
            return new ResponseEntity<>(
                    ApiResponse.error("Failed to retrieve demands: " + e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    @Override
    public ResponseEntity<ApiResponse<?>> getDemandById(UUID demandId) {
        try {
            if (demandId == null) {
                throw new ProjectExceptionHandler(
                        HttpStatus.BAD_REQUEST,
                        "DEMAND_ID_REQUIRED",
                        "Demand ID is required"
                );
            }

            Demand demand = demandRepository.findById(demandId)
                    .orElseThrow(() -> new ProjectExceptionHandler(
                            HttpStatus.NOT_FOUND,
                            "DEMAND_NOT_FOUND",
                            "Demand not found"
                    ));

            return new ResponseEntity<>(
                    ApiResponse.success("Demand retrieved successfully", demand),
                    HttpStatus.OK
            );

        } catch (ProjectExceptionHandler e) {
            return new ResponseEntity<>(
                    ApiResponse.error(e.getMessage()),
                    e.getStatus()
            );
        } catch (Exception e) {
            return new ResponseEntity<>(
                    ApiResponse.error("Failed to retrieve demand: " + e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    private void validateDemandTypeRules(Demand demand) {

        if (demand.getDemandType() == DemandType.REPLACEMENT) {

            if (demand.getOutgoingResource() == null) {
                throw new ProjectExceptionHandler(
                        HttpStatus.BAD_REQUEST,
                        "OUTGOING_RESOURCE_REQUIRED",
                        "Replacement demand must reference outgoing resource"
                );
            }
        }

        if (demand.getDemandType() == DemandType.NET_NEW) {

            if (demand.getDemandJustification() == null ||
                    demand.getDemandJustification().trim().length() < 20) {

                throw new ProjectExceptionHandler(
                        HttpStatus.BAD_REQUEST,
                        "JUSTIFICATION_REQUIRED",
                        "Net-New demand requires detailed business justification (min 20 characters)"
                );
            }
        }

        // Validate demand commitment rules
        if (demand.getDemandCommitment() == DemandCommitment.SOFT) {
            // No specific validation for soft demands currently
        }
    }


    private void applyDemandTypeRules(Demand demand) {

        if (demand.getDemandType() == DemandType.REPLACEMENT) {

            demand.setRequiresAdditionalApproval(false);

            if (demand.getDemandPriority() == null) {
                demand.setDemandPriority(PriorityLevel.HIGH);
            }
        }

        if (demand.getDemandType() == DemandType.NET_NEW) {

            demand.setRequiresAdditionalApproval(true);

            if (demand.getDemandPriority() == null) {
                demand.setDemandPriority(PriorityLevel.MEDIUM);
            }
        }
    }

    @Override
    public ResponseEntity<ApiResponse<?>> getDemandsByResourceManagerId(Long resourceManagerId) {
        try {
            // Validate resource manager ID
            if (resourceManagerId == null) {
                throw new ProjectExceptionHandler(
                        HttpStatus.BAD_REQUEST,
                        "RESOURCE_MANAGER_ID_REQUIRED",
                        "Resource Manager ID is required"
                );
            }

            // Fetch demands by resource manager ID (through project relationship)
            List<Demand> demands = demandRepository.findByProjectResourceManagerId(resourceManagerId);

            List<java.util.Map<String, Object>> formattedDemands = demands.stream()

                    // 🔥 STORY 3 – SORT BY DERIVED PRIORITY SCORE (DESC)
                    .sorted((d1, d2) -> Integer.compare(
                            calculatePriorityScore(d2),
                            calculatePriorityScore(d1)
                    ))

                    .map(demand -> {
                        java.util.Map<String, Object> demandInfo = new java.util.HashMap<>();
                        demandInfo.put("demandId", demand.getDemandId());
                        demandInfo.put("demandName",
                                demand.getDemandName() != null ? demand.getDemandName() : "Unnamed Demand");
                        demandInfo.put("projectId", demand.getProject().getPmsProjectId());
                        demandInfo.put("projectName", demand.getProject().getName());

                        // 👇 Visible for explainability
                        demandInfo.put("demandPriority", demand.getDemandPriority());
                        demandInfo.put("projectPriority", demand.getProject().getPriorityLevel());
                        demandInfo.put("priorityScore", calculatePriorityScore(demand));

                        return demandInfo;
                    })
                    .collect(java.util.stream.Collectors.toList());

            ApiResponse response = ApiResponse.success(
                    "Demands retrieved successfully",
                    formattedDemands
            );

            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (ProjectExceptionHandler e) {
            // Business validation failure
            ApiResponse response = ApiResponse.error(
                    e.getMessage()
            );
            return new ResponseEntity<>(response, e.getStatus());

        } catch (Exception e) {
            // Unexpected failure
            ApiResponse response = ApiResponse.error(
                    "Failed to retrieve demands: " + e.getMessage()
            );
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void attachMandatoryCompliances(Demand demand) {

        List<ProjectCompliance> compliances = projectComplianceRepo
                .findAllByProject_PmsProjectId(
                        demand.getProject().getPmsProjectId()
                ).orElse(List.of());

        for (ProjectCompliance compliance : compliances) {

            if (compliance.getActiveFlag() && compliance.getMandatoryFlag()) {

                if (compliance.getRequirementType() == RequirementType.SKILL &&
                        compliance.getClientCompliance() != null &&
                        compliance.getClientCompliance().getSkill() != null) {

                    demand.getRequiredSkills()
                            .add(compliance.getClientCompliance().getSkill());
                }

                if (compliance.getRequirementType() == RequirementType.CERTIFICATION &&
                        compliance.getClientCompliance() != null &&
                        compliance.getClientCompliance().getCertificate() != null) {

                    demand.getRequiredCertificates()
                            .add(compliance.getClientCompliance().getCertificate());
                }
            }
        }
    }
    private int mapPriorityToScore(PriorityLevel level) {
        if (level == null) return 0;

        return switch (level) {
            case CRITICAL -> 4;
            case HIGH -> 3;
            case MEDIUM -> 2;
            case LOW -> 1;
        };
    }
    private int calculatePriorityScore(Demand demand) {

        int demandScore = mapPriorityToScore(demand.getDemandPriority());
        int projectScore = mapPriorityToScore(demand.getProject().getPriorityLevel());

        // Demand priority has higher weight
        return (demandScore * 2) + projectScore;
    }
    
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void deleteSoftDemandsAfter30Days() {
        log.info("Starting soft demand cleanup process at {}", LocalDateTime.now());
        
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        
        List<Demand> softDemandsToDelete = demandRepository.findByDemandCommitmentAndCreatedAtBefore(
            DemandCommitment.SOFT, 
            cutoffDate
        );
        
        log.info("Found {} soft demands to delete (older than 30 days)", softDemandsToDelete.size());
        
        for (Demand demand : softDemandsToDelete) {
            try {
                demandRepository.delete(demand);
                log.info("Deleted soft demand: {} created on: {}", 
                    demand.getDemandId(), demand.getCreatedAt());
            } catch (Exception e) {
                log.error("Error deleting soft demand {}: {}", 
                    demand.getDemandId(), e.getMessage(), e);
            }
        }
        
        log.info("Completed soft demand cleanup process. Deleted {} demands", 
            softDemandsToDelete.size());
    }
}
