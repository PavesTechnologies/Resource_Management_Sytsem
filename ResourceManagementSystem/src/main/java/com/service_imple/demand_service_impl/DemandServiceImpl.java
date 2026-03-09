package com.service_imple.demand_service_impl;

import com.dto.ApiResponse;
import com.dto.UserDTO;
import com.dto.demand_dto.*;
import com.entity.demand_entities.Demand;
import com.entity.demand_entities.DemandSLA;
import com.entity.project_entities.Project;
import com.entity.project_entities.ProjectCompliance;
import com.entity.project_entities.ProjectSLA;
import com.entity.resource_entities.Resource;
import com.entity.skill_entities.DeliveryRoleExpectation;
import com.entity_enums.centralised_enums.PriorityLevel;
import com.entity_enums.client_enums.RequirementType;
//import com.entity_enums.skill_enums.DemandStatus;
import com.entity_enums.client_enums.SLAType;
import com.entity_enums.demand_enums.DemandCommitment;
import com.entity_enums.demand_enums.DemandStatus;
import com.entity_enums.demand_enums.DemandType;
import com.global_exception_handler.ProjectExceptionHandler;
import com.repo.demand_repo.DemandRepository;
import com.repo.demand_repo.DemandSLARepository;
import com.repo.project_repo.ProjectComplianceRepo;
import com.repo.project_repo.ProjectRepository;
import com.repo.project_repo.ProjectSLARepo;
import com.repo.resource_repo.ResourceRepository;
import com.repo.skill_repo.DeliveryRoleExpectationRepository;
import com.service_imple.project_service_impl.ProjectDemandValidationService;
import com.service_interface.demand_service_interface.DemandService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.entity_enums.demand_enums.DemandCommitment.SOFT;


@Service
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

    @Autowired
    private ProjectSLARepo projectSLARepository;

    @Autowired
    private DemandSLARepository demandSLARepository;

    @Override
    public ResponseEntity<ApiResponse<?>> createDemand(CreateDemandDTO dto, Long userId) {
        try {

            // 🔥 PRE-SUBMISSION VALIDATION - Early conflict detection
//            ResponseEntity<ApiResponse<DemandConflictValidationDTO>> validationResponse = validateDemandConflicts(dto);
//            if (validationResponse.getStatusCode().is2xxSuccessful() && validationResponse.getBody().getData() != null) {
//                DemandConflictValidationDTO validation = validationResponse.getBody().getData();
//
//                // Block submission if there are error-level conflicts
//                if (!validation.isCanSubmit()) {
//                    return ResponseEntity.badRequest().body(ApiResponse.error(
//                        "Demand submission blocked due to unresolved conflicts. Please resolve the following issues:\n" +
//                        formatConflictDetails(validation.getConflicts())
//                    ));
//                }
//
//                // Log warnings but allow submission
//                if (validation.isHasConflicts()) {
//                    System.out.println("WARNING: Demand submitted with conflicts: " + validation.getValidationMessage());
//                }
//            }

            // 🔐 Validate project eligibility
//            projectDemandValidationService.validateProjectForStaffing(dto.getProjectId());
            // Fetch Project
            Project project = projectRepository.findById(dto.getProjectId())
                    .orElseThrow(() -> new ProjectExceptionHandler(
                            HttpStatus.NOT_FOUND,
                            "PROJECT_NOT_FOUND",
                            "Project not found"
                    ));
            if (dto.getDemandStartDate().isBefore(project.getStartDate().toLocalDate()) || dto.getDemandEndDate().isAfter(project.getEndDate().toLocalDate())) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Demand date range is not in between project date range"));
            }

            // Fetch Role
            DeliveryRoleExpectation role;
            try {
                role = roleRepository.findById(dto.getDeliveryRole())
                        .orElseThrow(() -> new ProjectExceptionHandler(
                                HttpStatus.NOT_FOUND,
                                "ROLE_NOT_FOUND",
                                "Role not found with ID: " + dto.getDeliveryRole()
                        ));
            } catch (IllegalArgumentException e) {
                throw new ProjectExceptionHandler(
                        HttpStatus.BAD_REQUEST,
                        "INVALID_ROLE_ID",
                        "Invalid role ID format. Expected UUID format, received: " + dto.getDeliveryRole()
                );
            }

            // Validate required fields
            if (dto.getResourcesRequired() == null || dto.getResourcesRequired() < 1) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Resources required is mandatory and must be at least 1"));
            }
            
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

            // 🔥 Detect duplicate demands
            detectAndHandleDuplicateDemand(demand);

            // 🔥 Identify conflicting demands
            detectAndResolveConflicts(demand);

            // 🔥 Apply business rules
            applyDemandTypeRules(demand);

            // 🔹 Attach compliance requirements
            attachMandatoryCompliances(demand);

            Demand saved = demandRepository.save(demand);

            mapSlaToDemand(saved);

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

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<?>> deleteDemand(UUID demandId, UserDTO userDTO) {
        try {
            if (demandId == null) {
                throw new ProjectExceptionHandler(
                        HttpStatus.BAD_REQUEST,
                        "DEMAND_ID_REQUIRED",
                        "Demand ID is required"
                );
            }

            if (userDTO == null || userDTO.getId() == null) {
                throw new ProjectExceptionHandler(
                        HttpStatus.BAD_REQUEST,
                        "USER_REQUIRED",
                        "User information is required"
                );
            }

            Demand demand = demandRepository.findById(demandId)
                    .orElseThrow(() -> new ProjectExceptionHandler(
                            HttpStatus.NOT_FOUND,
                            "DEMAND_NOT_FOUND",
                            "Demand not found"
                    ));

            if (demand.getProject() == null) {
                throw new ProjectExceptionHandler(
                        HttpStatus.BAD_REQUEST,
                        "PROJECT_REQUIRED",
                        "Demand project information is missing"
                );
            }

            Long projectManagerId = demand.getProject().getProjectManagerId();
            if (projectManagerId == null || !projectManagerId.equals(userDTO.getId())) {
                throw new ProjectExceptionHandler(
                        HttpStatus.FORBIDDEN,
                        "ACCESS_DENIED",
                        "Only the Project Manager of this project can delete this demand"
                );
            }

            if (demand.getDemandStatus() == null) {
                throw new ProjectExceptionHandler(
                        HttpStatus.BAD_REQUEST,
                        "INVALID_STATE",
                        "Demand status is missing"
                );
            }

            if (demand.getDemandStatus() != DemandStatus.DRAFT &&
                    demand.getDemandStatus() != DemandStatus.REQUESTED &&
                    demand.getDemandStatus() != DemandStatus.CANCELLED &&
                    demand.getDemandCommitment() != DemandCommitment.SOFT) {

                throw new ProjectExceptionHandler(
                        HttpStatus.BAD_REQUEST,
                        "INVALID_STATE",
                        "PM can delete only DRAFT, REQUESTED, CANCELLED or SOFT commitment demands"
                );
            }

            demandSLARepository.deactivateByDemandId(demandId);
            demandRepository.delete(demand);

            return ResponseEntity.ok(ApiResponse.success("Demand deleted successfully", demandId));

        } catch (ProjectExceptionHandler e) {
            return new ResponseEntity<>(ApiResponse.error(e.getMessage()), e.getStatus());
        } catch (Exception e) {
            return new ResponseEntity<>(
                    ApiResponse.error("Failed to delete demand: " + e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
    
    private String formatConflictDetails(List<DemandConflictValidationDTO.ConflictDetail> conflicts) {
        StringBuilder sb = new StringBuilder();
        for (DemandConflictValidationDTO.ConflictDetail conflict : conflicts) {
            sb.append("• ").append(conflict.getDescription())
              .append(" (").append(conflict.getConflictType()).append(")\n")
              .append("  Impact: ").append(conflict.getImpact()).append("\n")
              .append("  Resolution: ").append(conflict.getSuggestedResolution()).append("\n\n");
        }
        return sb.toString();
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

        DemandType old = existing.getDemandType();

        // 🚫 Hard restriction:  demands cannot be modified
        if (existing.getDemandStatus() == DemandStatus.APPROVED ||
                existing.getDemandStatus() == DemandStatus.REJECTED ||
                existing.getDemandStatus() == DemandStatus.CANCELLED ||
                existing.getDemandStatus() == DemandStatus.FULFILLED) {

            throw new ProjectExceptionHandler(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_STATE",
                    "Approved, Rejected, Cancelled or Fulfilled demands cannot be modified"
            );
        }

        boolean criticalChanged = false;

        // ===== Critical Comparisons =====

        if (dto.getDemandStartDate() != null &&
                !dto.getDemandStartDate().equals(existing.getDemandStartDate())) {

            existing.setDemandStartDate(dto.getDemandStartDate());
            criticalChanged = true;
        }

        if (dto.getDemandEndDate() != null &&
                !dto.getDemandEndDate().equals(existing.getDemandEndDate())) {

            existing.setDemandEndDate(dto.getDemandEndDate());
            criticalChanged = true;
        }

        if (dto.getDemandPriority() != null &&
                dto.getDemandPriority() != existing.getDemandPriority()) {

            existing.setDemandPriority(dto.getDemandPriority());
            criticalChanged = true;
        }

        if (dto.getResourcesRequired() != null &&
                !dto.getResourcesRequired().equals(existing.getResourcesRequired())) {

            existing.setResourcesRequired(dto.getResourcesRequired());
            criticalChanged = true;
        }

        if (dto.getAllocationPercentage() != null &&
                !dto.getAllocationPercentage().equals(existing.getAllocationPercentage())) {

            existing.setAllocationPercentage(dto.getAllocationPercentage());
            criticalChanged = true;
        }

        if (dto.getDemandCommitment() != null &&
                dto.getDemandCommitment() != existing.getDemandCommitment()) {

            existing.setDemandCommitment(dto.getDemandCommitment());
            criticalChanged = true;
        }

        // ===== Non-Critical Updates =====

        if (dto.getDemandJustification() != null)
            existing.setDemandJustification(dto.getDemandJustification());

        if (dto.getDeliveryModel() != null)
            existing.setDeliveryModel(dto.getDeliveryModel());

        // Demand type update (if provided)
        if (dto.getDemandType() != null && !dto.getDemandType().equals(existing.getDemandType())) {
            existing.setDemandType(dto.getDemandType());
        }

        // 🔥 GOVERNANCE RULE
        if (criticalChanged &&
                existing.getDemandStatus() == DemandStatus.APPROVED) {

            existing.setDemandStatus(DemandStatus.REQUESTED);
            existing.setRequiresAdditionalApproval(true);
        }

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

        // Validate resulting dates are within project date range (final state validation)
        Project project = existing.getProject();
        if (project != null && project.getStartDate() != null && project.getEndDate() != null) {
            LocalDate projectStart = project.getStartDate().toLocalDate();
            LocalDate projectEnd = project.getEndDate().toLocalDate();
            if (existing.getDemandStartDate() != null && existing.getDemandStartDate().isBefore(projectStart)) {
                throw new ProjectExceptionHandler(
                        HttpStatus.BAD_REQUEST,
                        "INVALID_DATE_RANGE",
                        "Demand start date must be within project date range"
                );
            }
            if (existing.getDemandEndDate() != null && existing.getDemandEndDate().isAfter(projectEnd)) {
                throw new ProjectExceptionHandler(
                        HttpStatus.BAD_REQUEST,
                        "INVALID_DATE_RANGE",
                        "Demand end date must be within project date range"
                );
            }
        }

        // Validate demand type rules based on resulting entity state
        validateDemandTypeRules(existing);

        if (dto.getDemandType() != null && !old.equals(dto.getDemandType())) {
            remapSla(existing);
        }

        demandRepository.save(existing);

        // Optional message change
        if (criticalChanged) {
            return ResponseEntity.ok(
                    ApiResponse.success(
                            "Critical changes detected. Demand moved to REQUESTED state.",
                            existing.getDemandId()
                    )
            );
        }

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

            List<Demand> demands = demandRepository
                    .findByProject_PmsProjectId(projectId)
                    .stream()
                    .filter(d -> d.getDemandStatus() != DemandStatus.CANCELLED)
                    .collect(Collectors.toList());

            List<DemandDetailResponseDTO> formattedDemands = demands.stream()
                    .sorted((d1, d2) -> Integer.compare(
                            calculatePriorityScore(d2),
                            calculatePriorityScore(d1)
                    ))
                    .map(demand -> {
                        Optional<DemandSLA> demandSLAOpt = demandSLARepository.findByDemand_DemandIdAndActiveFlagTrue(demand.getDemandId());

                        DemandDetailResponseDTO demandInfo = DemandDetailResponseDTO.builder()
                                .clientId(demand.getProject().getClientId())
                                .clientName(demand.getProject().getClient() != null ?
                                        demand.getProject().getClient().getClientName() : "Unknown Client")
                                .projectId(demand.getProject().getPmsProjectId())
                                .projectName(demand.getProject().getName())
                                .deliveryRole(demand.getRole().toString())
                                .demandJustification(demand.getDemandJustification())
                                .minExp(demand.getMinExp())
                                .resourceRequired(demand.getResourcesRequired())
                                .allocation(demand.getAllocationPercentage())
                                .demandId(demand.getDemandId())
                                .demandName(demand.getDemandName() != null ? demand.getDemandName() : "Unnamed Demand")
                                .demandPriority(demand.getDemandPriority() != null ? demand.getDemandPriority().toString() : "UNKNOWN")
                                .demandStatus(demand.getDemandStatus() != null ? demand.getDemandStatus().toString() : "UNKNOWN")
                                .demandType(demand.getDemandType() != null ? demand.getDemandType().toString() : "UNKNOWN")
                                .deliveryModel(demand.getDeliveryModel() != null ? demand.getDeliveryModel().toString() : "UNKNOWN")
                                .demandStartDate(demand.getDemandStartDate())
                                .demandEndDate(demand.getDemandEndDate())
                                .priorityScore(calculatePriorityScore(demand))
                                .build();

                        if (demandSLAOpt.isPresent()) {
                            DemandSLA demandSLA = demandSLAOpt.get();
                            LocalDate today = LocalDate.now();

                            demandInfo.setDemandSlaId(demandSLA.getDemandSlaId());
                            demandInfo.setSlaType(demandSLA.getSlaType() != null ? demandSLA.getSlaType().toString() : "UNKNOWN");
                            demandInfo.setSlaDurationDays(demandSLA.getSlaDurationDays());
                            demandInfo.setWarningThresholdDays(demandSLA.getWarningThresholdDays());
                            demandInfo.setSlaCreatedAt(demandSLA.getCreatedAt());
                            demandInfo.setSlaDueAt(demandSLA.getDueAt());

                            if (demandSLA.getDueAt() != null) {
                                if (today.isAfter(demandSLA.getDueAt())) {
                                    demandInfo.setSlaBreached(true);
                                    demandInfo.setOverdueDays(ChronoUnit.DAYS.between(demandSLA.getDueAt(), today));
                                    demandInfo.setRemainingDays(0);
                                } else {
                                    demandInfo.setSlaBreached(false);
                                    demandInfo.setRemainingDays(ChronoUnit.DAYS.between(today, demandSLA.getDueAt()));
                                    demandInfo.setOverdueDays(0);
                                }
                            }
                        }

                        return demandInfo;
                    })
                    .collect(java.util.stream.Collectors.toList());

            return new ResponseEntity<>(
                    ApiResponse.success("Demands retrieved successfully", formattedDemands),
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

            // Get SLA details for the demand
            Optional<DemandSLA> demandSLAOpt = demandSLARepository.findByDemand_DemandIdAndActiveFlagTrue(demand.getDemandId());
            
            // Create a response map to include both demand and SLA data
            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("demand", demand);
            
            // Add SLA details if present
            if (demandSLAOpt.isPresent()) {
                DemandSLA demandSLA = demandSLAOpt.get();
                LocalDate today = LocalDate.now();
                
                java.util.Map<String, Object> slaData = new java.util.HashMap<>();
                slaData.put("demandSlaId", demandSLA.getDemandSlaId());
                slaData.put("slaType", demandSLA.getSlaType() != null ? demandSLA.getSlaType().toString() : "UNKNOWN");
                slaData.put("slaDurationDays", demandSLA.getSlaDurationDays());
                slaData.put("warningThresholdDays", demandSLA.getWarningThresholdDays());
                slaData.put("slaCreatedAt", demandSLA.getCreatedAt());
                slaData.put("slaDueAt", demandSLA.getDueAt());
                
                // Calculate SLA status
                if (demandSLA.getDueAt() != null) {
                    if (today.isAfter(demandSLA.getDueAt())) {
                        slaData.put("slaBreached", true);
                        slaData.put("overdueDays", java.time.temporal.ChronoUnit.DAYS.between(demandSLA.getDueAt(), today));
                        slaData.put("remainingDays", 0);
                    } else {
                        slaData.put("slaBreached", false);
                        slaData.put("remainingDays", java.time.temporal.ChronoUnit.DAYS.between(today, demandSLA.getDueAt()));
                        slaData.put("overdueDays", 0);
                    }
                }
                
                response.put("sla", slaData);
            } else {
                response.put("sla", null);
            }

            return new ResponseEntity<>(
                    ApiResponse.success("Demand retrieved successfully", response),
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

            List<DemandDetailResponseDTO> formattedDemands = demands.stream()

                    // 🔥 STORY 3 – SORT BY DERIVED PRIORITY SCORE (DESC)
                    .sorted((d1, d2) -> Integer.compare(
                            calculatePriorityScore(d2),
                            calculatePriorityScore(d1)
                    ))

                    .map(demand -> {
                        // Get SLA details for the demand
                        Optional<DemandSLA> demandSLAOpt = demandSLARepository.findByDemand_DemandIdAndActiveFlagTrue(demand.getDemandId());
                        
                        DemandDetailResponseDTO demandInfo = DemandDetailResponseDTO.builder()
                                .clientId(demand.getProject().getClientId())
                                .clientName(demand.getProject().getClient() != null ? 
                                    demand.getProject().getClient().getClientName() : "Unknown Client")
                                .projectId(demand.getProject().getPmsProjectId())
                                .projectName(demand.getProject().getName())
                                .deliveryRole(demand.getRole().getRole().getRoleName())
                                .demandJustification(demand.getDemandJustification())
                                .minExp(demand.getMinExp())
                                .resourceRequired(demand.getResourcesRequired())
                                .allocation(demand.getAllocationPercentage())
                                .demandId(demand.getDemandId())
                                .demandName(demand.getDemandName() != null ? demand.getDemandName() : "Unnamed Demand")
                                .demandPriority(demand.getDemandPriority() != null ? demand.getDemandPriority().toString() : "UNKNOWN")
                                .demandStatus(demand.getDemandStatus() != null ? demand.getDemandStatus().toString() : "UNKNOWN")
                                .demandType(demand.getDemandType() != null ? demand.getDemandType().toString() : "UNKNOWN")
                                .deliveryModel(demand.getDeliveryModel() != null ? demand.getDeliveryModel().toString() : "UNKNOWN")
                                .demandStartDate(demand.getDemandStartDate())
                                .demandEndDate(demand.getDemandEndDate())
                                .priorityScore(calculatePriorityScore(demand))
                                .build();

                        // Add SLA details if present
                        if (demandSLAOpt.isPresent()) {
                            DemandSLA demandSLA = demandSLAOpt.get();
                            LocalDate today = LocalDate.now();
                            
                            demandInfo.setDemandSlaId(demandSLA.getDemandSlaId());
                            demandInfo.setSlaType(demandSLA.getSlaType() != null ? demandSLA.getSlaType().toString() : "UNKNOWN");
                            demandInfo.setSlaDurationDays(demandSLA.getSlaDurationDays());
                            demandInfo.setWarningThresholdDays(demandSLA.getWarningThresholdDays());
                            demandInfo.setSlaCreatedAt(demandSLA.getCreatedAt());
                            demandInfo.setSlaDueAt(demandSLA.getDueAt());
                            
                            // Calculate SLA status
                            if (demandSLA.getDueAt() != null) {
                                if (today.isAfter(demandSLA.getDueAt())) {
                                    demandInfo.setSlaBreached(true);
                                    demandInfo.setOverdueDays(java.time.temporal.ChronoUnit.DAYS.between(demandSLA.getDueAt(), today));
                                    demandInfo.setRemainingDays(0);
                                } else {
                                    demandInfo.setSlaBreached(false);
                                    demandInfo.setRemainingDays(java.time.temporal.ChronoUnit.DAYS.between(today, demandSLA.getDueAt()));
                                    demandInfo.setOverdueDays(0);
                                }
                            }
                        }

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

    @Override
    public ResponseEntity<ApiResponse<?>> getDemandsByCreatedBy(Long createdBy) {
        return getDemandsByCreatedByAndProjectId(createdBy, null);
    }

    @Override
    public ResponseEntity<ApiResponse<?>> getDemandsByCreatedByAndProjectId(Long createdBy, Long projectId) {
        try {
            // Validate created by ID
            if (createdBy == null) {
                throw new ProjectExceptionHandler(
                        HttpStatus.BAD_REQUEST,
                        "CREATED_BY_ID_REQUIRED",
                        "Created By ID is required"
                );
            }

            if (projectId == null) {
                throw new ProjectExceptionHandler(
                        HttpStatus.BAD_REQUEST,
                        "PROJECT_ID_REQUIRED",
                        "Project ID is required"
                );
            }

            // Fetch demands by created by ID and optionally by project ID
            List<Demand> demands;
            demands = demandRepository.findByCreatedByAndProjectId(createdBy, projectId);

            if (demands == null || demands.isEmpty()) {
                throw new ProjectExceptionHandler(
                        HttpStatus.NOT_FOUND,
                        "NO_DEMANDS_FOUND",
                        "No demands found for this project"
                );
            }

            List<DemandDetailResponseDTO> formattedDemands = demands.stream()

                    // 🔥 STORY 3 – SORT BY DERIVED PRIORITY SCORE (DESC)
                    .sorted((d1, d2) -> Integer.compare(
                            calculatePriorityScore(d2),
                            calculatePriorityScore(d1)
                    ))

                    .map(demand -> {
                        // Get SLA details for the demand
                        Optional<DemandSLA> demandSLAOpt = demandSLARepository.findByDemand_DemandIdAndActiveFlagTrue(demand.getDemandId());
                        
                        DemandDetailResponseDTO demandInfo = DemandDetailResponseDTO.builder()
                                .clientId(demand.getProject().getClientId())
                                .clientName(demand.getProject().getClient() != null ? 
                                    demand.getProject().getClient().getClientName() : "Unknown Client")
                                .projectId(demand.getProject().getPmsProjectId())
                                .projectName(demand.getProject().getName())
                                .deliveryRole(demand.getRole().getRole().getRoleName())
                                .demandJustification(demand.getDemandJustification())
                                .minExp(demand.getMinExp())
                                .resourceRequired(demand.getResourcesRequired())
                                .allocation(demand.getAllocationPercentage())
                                .demandId(demand.getDemandId())
                                .demandName(demand.getDemandName() != null ? demand.getDemandName() : "Unnamed Demand")
                                .demandPriority(demand.getDemandPriority() != null ? demand.getDemandPriority().toString() : "UNKNOWN")
                                .demandStatus(demand.getDemandStatus() != null ? demand.getDemandStatus().toString() : "UNKNOWN")
                                .demandType(demand.getDemandType() != null ? demand.getDemandType().toString() : "UNKNOWN")
                                .deliveryModel(demand.getDeliveryModel() != null ? demand.getDeliveryModel().toString() : "UNKNOWN")
                                .demandStartDate(demand.getDemandStartDate())
                                .demandEndDate(demand.getDemandEndDate())
                                .priorityScore(calculatePriorityScore(demand))
                                .build();

                        // Add SLA details if present
                        if (demandSLAOpt.isPresent()) {
                            DemandSLA demandSLA = demandSLAOpt.get();
                            LocalDate today = LocalDate.now();
                            
                            demandInfo.setDemandSlaId(demandSLA.getDemandSlaId());
                            demandInfo.setSlaType(demandSLA.getSlaType() != null ? demandSLA.getSlaType().toString() : "UNKNOWN");
                            demandInfo.setSlaDurationDays(demandSLA.getSlaDurationDays());
                            demandInfo.setWarningThresholdDays(demandSLA.getWarningThresholdDays());
                            demandInfo.setSlaCreatedAt(demandSLA.getCreatedAt());
                            demandInfo.setSlaDueAt(demandSLA.getDueAt());
                            
                            // Calculate SLA status
                            if (demandSLA.getDueAt() != null) {
                                if (today.isAfter(demandSLA.getDueAt())) {
                                    demandInfo.setSlaBreached(true);
                                    demandInfo.setOverdueDays(java.time.temporal.ChronoUnit.DAYS.between(demandSLA.getDueAt(), today));
                                    demandInfo.setRemainingDays(0);
                                } else {
                                    demandInfo.setSlaBreached(false);
                                    demandInfo.setRemainingDays(java.time.temporal.ChronoUnit.DAYS.between(today, demandSLA.getDueAt()));
                                    demandInfo.setOverdueDays(0);
                                }
                            }
                        }

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

    private SLAType mapDemandTypeToSLAType(DemandType demandType) {
        if (demandType == null) return null;
        
        return switch (demandType) {
            case NET_NEW -> SLAType.NET_NEW;
            case REPLACEMENT -> SLAType.REPLACEMENT;
            case BACKFILL -> SLAType.BACKFILL;
            case EMERGENCY -> SLAType.EMERGENCY;
        };
    }

    @Transactional
    public void mapSlaToDemand(Demand demand) {

        if (demand.getDemandCommitment()!=DemandCommitment.CONFIRMED) {
            return;
        }

        SLAType slaType = mapDemandTypeToSLAType(demand.getDemandType());
        if (slaType == null) {
            return; // No valid SLA type mapping
        }

        Optional<ProjectSLA> projectSlaOpt =
                projectSLARepository.findByProjectAndSlaTypeAndActiveFlagTrue(
                        demand.getProject(),
                        slaType
                );

        if (projectSlaOpt.isEmpty()) {
            return; // No SLA defined for this type
        }

        ProjectSLA projectSLA = projectSlaOpt.get();

        LocalDate now = LocalDate.now();
        LocalDate dueAt = now.plusDays(projectSLA.getSlaDurationDays());

        DemandSLA demandSLA = DemandSLA.builder()
                .demand(demand)
                .projectSLA(projectSLA)
                .slaType(projectSLA.getSlaType())
                .slaDurationDays(projectSLA.getSlaDurationDays())
                .warningThresholdDays(projectSLA.getWarningThresholdDays())
                .createdAt(now)
                .dueAt(dueAt)
                .activeFlag(true)
                .build();

        demandSLARepository.save(demandSLA);
    }

    @Transactional
    public void remapSla(Demand demand) {

        int updatedRows = demandSLARepository
                .deactivateByDemandId(demand.getDemandId());

        if (updatedRows == 0) {
            throw new RuntimeException("No Active Demand SLA Found with the ID.");
        }

        mapSlaToDemand(demand);
    }

    // ============================
    // TASK 1: DUPLICATE DEMAND DETECTION
    // ============================
    
    private void detectAndHandleDuplicateDemand(Demand demand) {
        // Check for exact duplicates
        List<Demand> existingDemands = demandRepository.findByProject_PmsProjectId(demand.getProject().getPmsProjectId());
        
        for (Demand existing : existingDemands) {
            if (isExactDuplicate(demand, existing)) {
                throw new ProjectExceptionHandler(
                        HttpStatus.CONFLICT,
                        "DUPLICATE_DEMAND",
                        "Exact duplicate demand exists for project: " + demand.getProject().getName() + 
                        ", role: " + demand.getRole().getRole().getRoleName() + 
                        ", dates: " + demand.getDemandStartDate() + " to " + demand.getDemandEndDate()
                );
            }
        }
        
        // Check for similar demands (warning)
        List<Demand> similarDemands = findSimilarDemands(demand, existingDemands);
        if (!similarDemands.isEmpty()) {
            // Log warning but allow creation
            System.out.println("WARNING: Similar demands found for project " + demand.getProject().getName() + 
                             ". Consider reviewing existing demands before proceeding.");
        }
    }
    
    private boolean isExactDuplicate(Demand newDemand, Demand existing) {
        return newDemand.getProject().getPmsProjectId().equals(existing.getProject().getPmsProjectId()) &&
               newDemand.getRole().getId().equals(existing.getRole().getId()) &&
               newDemand.getDemandStartDate().equals(existing.getDemandStartDate()) &&
               newDemand.getDemandEndDate().equals(existing.getDemandEndDate()) &&
               newDemand.getAllocationPercentage().equals(existing.getAllocationPercentage());
    }
    
    private List<Demand> findSimilarDemands(Demand newDemand, List<Demand> existingDemands) {
        return existingDemands.stream()
                .filter(existing -> isSimilarDemand(newDemand, existing))
                .collect(java.util.stream.Collectors.toList());
    }
    
    private boolean isSimilarDemand(Demand newDemand, Demand existing) {
        // Same project and role with overlapping dates
        return newDemand.getProject().getPmsProjectId().equals(existing.getProject().getPmsProjectId()) &&
               newDemand.getRole().getId().equals(existing.getRole().getId()) &&
               isDateRangeOverlapping(newDemand.getDemandStartDate(), newDemand.getDemandEndDate(), 
                                     existing.getDemandStartDate(), existing.getDemandEndDate());
    }
    
    private boolean isDateRangeOverlapping(LocalDate start1, LocalDate end1, LocalDate start2, LocalDate end2) {
        return !start1.isAfter(end2) && !start2.isAfter(end1);
    }

    // ============================
    // TASK 2: CONFLICT DETECTION
    // ============================
    
    private void detectAndResolveConflicts(Demand demand) {
        // Detect resource allocation conflicts
        detectAllocationConflicts(demand);
        
        // Detect timeline conflicts
        detectTimelineConflicts(demand);
        
        // Detect skill requirement conflicts
        detectSkillConflicts(demand);
    }
    
    private void detectAllocationConflicts(Demand demand) {
        List<Demand> projectDemands = demandRepository.findByProject_PmsProjectId(demand.getProject().getPmsProjectId());
        
        // Calculate total allocation for overlapping demands
        int totalAllocation = demand.getAllocationPercentage();
        
        for (Demand existing : projectDemands) {
            if (isDateRangeOverlapping(demand.getDemandStartDate(), demand.getDemandEndDate(),
                                      existing.getDemandStartDate(), existing.getDemandEndDate()) &&
                existing.getDemandStatus() != DemandStatus.CANCELLED &&
                existing.getDemandStatus() != DemandStatus.REJECTED) {
                
                totalAllocation += existing.getAllocationPercentage();
            }
        }
        
        // Conflict if total allocation exceeds 100%
        if (totalAllocation > 100) {
            // Apply conflict resolution rule
            resolveAllocationConflict(demand, totalAllocation);
        }
    }
    
    private void resolveAllocationConflict(Demand demand, int totalAllocation) {
        // Rule: High priority demands take precedence
        if (demand.getDemandPriority() == PriorityLevel.CRITICAL || 
            demand.getDemandPriority() == PriorityLevel.HIGH) {
            
            // Reduce allocation of lower priority existing demands
            List<Demand> conflictingDemands = findConflictingDemandsForReduction(demand);
            
            for (Demand conflicting : conflictingDemands) {
                if (conflicting.getDemandPriority() == PriorityLevel.LOW || 
                    conflicting.getDemandPriority() == PriorityLevel.MEDIUM) {
                    
                    // Reduce allocation to make room
                    int reduction = Math.min(conflicting.getAllocationPercentage(), totalAllocation - 100);
                    conflicting.setAllocationPercentage(conflicting.getAllocationPercentage() - reduction);
                    conflicting.setRequiresAdditionalApproval(true);
                    demandRepository.save(conflicting);
                    
                    System.out.println("CONFLICT RESOLVED: Reduced allocation for demand " + conflicting.getDemandName() + 
                                     " by " + reduction + "% to accommodate high priority demand.");
                    
                    totalAllocation -= reduction;
                    if (totalAllocation <= 100) break;
                }
            }
        } else {
            // Lower priority demand - require approval
            demand.setRequiresAdditionalApproval(true);
            System.out.println("ALLOCATION CONFLICT: Demand requires additional approval due to resource constraints. Total allocation: " + totalAllocation + "%");
        }
    }
    
    private List<Demand> findConflictingDemandsForReduction(Demand newDemand) {
        return demandRepository.findByProject_PmsProjectId(newDemand.getProject().getPmsProjectId())
                .stream()
                .filter(existing -> 
                    isDateRangeOverlapping(newDemand.getDemandStartDate(), newDemand.getDemandEndDate(),
                                          existing.getDemandStartDate(), existing.getDemandEndDate()) &&
                    existing.getDemandStatus() != DemandStatus.CANCELLED &&
                    existing.getDemandStatus() != DemandStatus.REJECTED &&
                    (existing.getDemandPriority() == PriorityLevel.LOW || 
                     existing.getDemandPriority() == PriorityLevel.MEDIUM))
                .sorted((d1, d2) -> d1.getDemandPriority().compareTo(d2.getDemandPriority()))
                .collect(java.util.stream.Collectors.toList());
    }
    
    private void detectTimelineConflicts(Demand demand) {
        List<Demand> projectDemands = demandRepository.findByProject_PmsProjectId(demand.getProject().getPmsProjectId());
        
        for (Demand existing : projectDemands) {
            if (existing.getDemandStatus() != DemandStatus.CANCELLED &&
                existing.getDemandStatus() != DemandStatus.REJECTED &&
                isDateRangeOverlapping(demand.getDemandStartDate(), demand.getDemandEndDate(),
                                      existing.getDemandStartDate(), existing.getDemandEndDate()) &&
                demand.getRole().getId().equals(existing.getRole().getId())) {
                
                // Timeline conflict detected - resolve based on priority
                resolveTimelineConflict(demand, existing);
            }
        }
    }
    
    private void resolveTimelineConflict(Demand newDemand, Demand existing) {
        // Rule: Higher priority demand gets preferred dates
        int comparison = newDemand.getDemandPriority().compareTo(existing.getDemandPriority());
        
        if (comparison > 0) {
            // New demand has higher priority - adjust existing demand
            System.out.println("TIMELINE CONFLICT RESOLVED: New demand " + newDemand.getDemandName() + 
                             " has higher priority than existing demand " + existing.getDemandName());
            
            // Mark existing demand for review
            existing.setRequiresAdditionalApproval(true);
            demandRepository.save(existing);
        } else if (comparison < 0) {
            // Existing demand has higher priority - new demand needs adjustment
            newDemand.setRequiresAdditionalApproval(true);
            System.out.println("TIMELINE CONFLICT: New demand requires approval due to conflict with higher priority existing demand.");
        } else {
            // Same priority - both require review
            newDemand.setRequiresAdditionalApproval(true);
            existing.setRequiresAdditionalApproval(true);
            demandRepository.save(existing);
            System.out.println("TIMELINE CONFLICT: Same priority demands conflict - both require review.");
        }
    }
    
    private void detectSkillConflicts(Demand demand) {
        // Check for incompatible skill requirements within the same role
        if (demand.getRequiredSkills().size() > 10) {
            // Rule: Limit skill requirements to prevent over-specification
            demand.setRequiresAdditionalApproval(true);
            System.out.println("SKILL CONFLICT: Too many skills required (" + demand.getRequiredSkills().size() + 
                             "). Consider consolidating or prioritizing skills.");
        }
        
        // Check for conflicting skill levels (if implemented)
        // This would require skill level tracking in the entity
    }

    // ============================
    // TASK 3: CONFLICT RESOLUTION RULES
    // ============================
    
    public ResponseEntity<ApiResponse<?>> resolveDemandConflicts(Long projectId) {
        try {
            List<Demand> projectDemands = demandRepository.findByProject_PmsProjectId(projectId);
            
            int resolvedCount = 0;
            int escalatedCount = 0;
            
            for (Demand demand : projectDemands) {
                if (demand.getRequiresAdditionalApproval()) {
                    // Apply resolution rules
                    ConflictResolutionResult result = applyConflictResolutionRules(demand);
                    
                    if (result == ConflictResolutionResult.RESOLVED) {
                        resolvedCount++;
                    } else if (result == ConflictResolutionResult.ESCALATED) {
                        escalatedCount++;
                    }
                }
            }
            
            return ResponseEntity.ok(ApiResponse.success(
                "Conflict resolution completed. Resolved: " + resolvedCount + ", Escalated: " + escalatedCount,
                java.util.Map.of("resolved", resolvedCount, "escalated", escalatedCount)
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to resolve conflicts: " + e.getMessage()));
        }
    }
    
    private ConflictResolutionResult applyConflictResolutionRules(Demand demand) {
        // Rule 1: Critical demands auto-approve
        if (demand.getDemandPriority() == PriorityLevel.CRITICAL) {
            demand.setRequiresAdditionalApproval(false);
            demandRepository.save(demand);
            return ConflictResolutionResult.RESOLVED;
        }
        
        // Rule 2: Emergency demands auto-approve
        if (demand.getDemandType() == DemandType.EMERGENCY) {
            demand.setRequiresAdditionalApproval(false);
            demandRepository.save(demand);
            return ConflictResolutionResult.RESOLVED;
        }
        
        // Rule 3: Low allocation demands (<20%) auto-approve
        if (demand.getAllocationPercentage() < 20) {
            demand.setRequiresAdditionalApproval(false);
            demandRepository.save(demand);
            return ConflictResolutionResult.RESOLVED;
        }
        
        // Rule 4: High allocation (>80%) escalate
        if (demand.getAllocationPercentage() > 80) {
            return ConflictResolutionResult.ESCALATED;
        }
        
        // Default: keep as requires approval
        return ConflictResolutionResult.MANUAL_REVIEW;
    }
    
    private enum ConflictResolutionResult {
        RESOLVED, ESCALATED, MANUAL_REVIEW
    }

    // ============================
    // EARLY CONFLICT VALIDATION
    // ============================
    
    @Override
    public ResponseEntity<ApiResponse<DemandConflictValidationDTO>> validateDemandConflicts(CreateDemandDTO dto) {
        try {
            DemandConflictValidationDTO validation = new DemandConflictValidationDTO();
            validation.setConflicts(new ArrayList<>());
            
            // Validate project exists
            Project project = projectRepository.findById(dto.getProjectId())
                    .orElseThrow(() -> new ProjectExceptionHandler(
                            HttpStatus.NOT_FOUND,
                            "PROJECT_NOT_FOUND",
                            "Project not found"
                    ));
            
            // Validate role exists
            DeliveryRoleExpectation role;
            try {
                role = roleRepository.findById(dto.getDeliveryRole())
                        .orElseThrow(() -> new ProjectExceptionHandler(
                                HttpStatus.NOT_FOUND,
                                "ROLE_NOT_FOUND",
                                "Role not found with ID: " + dto.getDeliveryRole()
                        ));
            } catch (IllegalArgumentException e) {
                throw new ProjectExceptionHandler(
                        HttpStatus.BAD_REQUEST,
                        "INVALID_ROLE_ID",
                        "Invalid role ID format. Expected UUID format, received: " + dto.getDeliveryRole()
                );
            }
            
            // Create temporary demand for validation
            Demand tempDemand = createTempDemand(dto, project, role);
            
            // Check all conflict types
            validateCapacityLimits(tempDemand, validation);
            validateContradictoryRoles(tempDemand, validation);
            validateTimelineConflicts(tempDemand, validation);
            validateSkillRequirements(tempDemand, validation);
            validateBusinessRules(tempDemand, validation);
            
            // Determine if submission is allowed
            boolean hasErrors = validation.getConflicts().stream()
                    .anyMatch(conflict -> "ERROR".equals(conflict.getSeverity()));
            
            validation.setHasConflicts(!validation.getConflicts().isEmpty());
            validation.setCanSubmit(!hasErrors);
            
            if (hasErrors) {
                validation.setValidationMessage("Demand has blocking conflicts that must be resolved before submission.");
            } else if (validation.getConflicts().isEmpty()) {
                validation.setValidationMessage("No conflicts detected. Demand can be submitted.");
            } else {
                validation.setValidationMessage("Demand has warnings but can be submitted.");
            }
            
            return ResponseEntity.ok(ApiResponse.success("Validation completed", validation));
            
        } catch (ProjectExceptionHandler e) {
            return new ResponseEntity<>(
                    ApiResponse.error(e.getMessage()),
                    e.getStatus()
            );
        } catch (Exception e) {
            return new ResponseEntity<>(
                    ApiResponse.error("Validation failed: " + e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
    
    private Demand createTempDemand(CreateDemandDTO dto, Project project, DeliveryRoleExpectation role) {
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
        demand.setDemandCommitment(dto.getDemandCommitment());
        demand.setRequiresAdditionalApproval(dto.getRequiresAdditionalApproval());
        if (dto.getDemandType() == DemandType.REPLACEMENT && dto.getOutgoingResourceId() != null) {
            Resource resource = resourceRepository
                    .findById(dto.getOutgoingResourceId())
                    .orElse(null);

            demand.setOutgoingResource(resource);
        }
        return demand;
    }
    
    private void validateCapacityLimits(Demand demand, DemandConflictValidationDTO validation) {
        List<Demand> existingDemands = demandRepository.findByProject_PmsProjectId(demand.getProject().getPmsProjectId());
        
        // Calculate total allocation for overlapping demands
        int totalAllocation = demand.getAllocationPercentage();
        
        for (Demand existing : existingDemands) {
            if (isDateRangeOverlapping(demand.getDemandStartDate(), demand.getDemandEndDate(),
                                      existing.getDemandStartDate(), existing.getDemandEndDate()) &&
                existing.getDemandStatus() != DemandStatus.CANCELLED &&
                existing.getDemandStatus() != DemandStatus.REJECTED) {
                
                totalAllocation += existing.getAllocationPercentage();
            }
        }
        
        // Check capacity limits
        if (totalAllocation > 100) {
            validation.getConflicts().add(
                DemandConflictValidationDTO.ConflictDetail.error(
                    "CAPACITY_EXCEEDED",
                    "Project resource allocation exceeds 100% capacity",
                    "Current allocation: " + totalAllocation + "% (Maximum allowed: 100%)",
                    "Reduce allocation percentage or adjust demand dates to avoid overlap"
                )
            );
        } else if (totalAllocation > 90) {
            validation.getConflicts().add(
                DemandConflictValidationDTO.ConflictDetail.warning(
                    "CAPACITY_WARNING",
                    "Project resource allocation is approaching capacity limits",
                    "Current allocation: " + totalAllocation + "% (Recommended maximum: 90%)",
                    "Consider monitoring resource utilization closely"
                )
            );
        }
    }
    
    private void validateContradictoryRoles(Demand demand, DemandConflictValidationDTO validation) {
        List<Demand> existingDemands = demandRepository.findByProject_PmsProjectId(demand.getProject().getPmsProjectId());
        
        for (Demand existing : existingDemands) {
            if (existing.getDemandStatus() != DemandStatus.CANCELLED &&
                existing.getDemandStatus() != DemandStatus.REJECTED &&
                isDateRangeOverlapping(demand.getDemandStartDate(), demand.getDemandEndDate(),
                                      existing.getDemandStartDate(), existing.getDemandEndDate())) {
                
                // Check for contradictory role combinations
                if (hasContradictoryRoles(demand.getRole(), existing.getRole())) {
                    validation.getConflicts().add(
                        DemandConflictValidationDTO.ConflictDetail.error(
                            "CONTRADICTORY_ROLES",
                            "Demand has contradictory role requirements with existing demand",
                            "Role '" + demand.getRole().getRole().getRoleName() + "' conflicts with role '" + existing.getRole().getRole().getRoleName() + "'",
                            "Review role assignments and ensure they are compatible"
                        )
                    );
                }
                
                // Check for same resource assignment to multiple full-time roles
                if (demand.getRole().getId().equals(existing.getRole().getId()) &&
                    demand.getAllocationPercentage() > 50 && existing.getAllocationPercentage() > 50) {
                    
                    validation.getConflicts().add(
                        DemandConflictValidationDTO.ConflictDetail.error(
                            "DUPLICATE_FULL_TIME_ROLE",
                            "Same role assigned to multiple resources with high allocation",
                            "Role '" + demand.getRole().getRole().getRoleName() + "' has conflicting high allocations",
                            "Reduce allocation or use different role designation"
                        )
                    );
                }
            }
        }
    }
    
    private boolean hasContradictoryRoles(DeliveryRoleExpectation role1, DeliveryRoleExpectation role2) {
        // Define contradictory role combinations
        // This is a simplified example - you'd need to define actual business rules
        String role1Name = role1.getRole().getRoleName().toLowerCase();
        String role2Name = role2.getRole().getRoleName().toLowerCase();
        
        // Example: Project Manager and Team Lead might be contradictory if both are full-time
        if ((role1Name.contains("project manager") && role2Name.contains("team lead")) ||
            (role1Name.contains("team lead") && role2Name.contains("project manager"))) {
            return true;
        }
        
        // Example: Developer and Tester for same resource might be contradictory
        if ((role1Name.contains("developer") && role2Name.contains("tester")) ||
            (role1Name.contains("tester") && role2Name.contains("developer"))) {
            return true;
        }
        
        return false;
    }
    
    private void validateTimelineConflicts(Demand demand, DemandConflictValidationDTO validation) {
        List<Demand> existingDemands = demandRepository.findByProject_PmsProjectId(demand.getProject().getPmsProjectId());
        
        for (Demand existing : existingDemands) {
            if (existing.getDemandStatus() != DemandStatus.CANCELLED &&
                existing.getDemandStatus() != DemandStatus.REJECTED &&
                isDateRangeOverlapping(demand.getDemandStartDate(), demand.getDemandEndDate(),
                                      existing.getDemandStartDate(), existing.getDemandEndDate()) &&
                demand.getRole().getId().equals(existing.getRole().getId())) {
                
                validation.getConflicts().add(
                    DemandConflictValidationDTO.ConflictDetail.error(
                        "TIMELINE_CONFLICT",
                        "Demand timeline conflicts with existing demand for same role",
                        "Role '" + demand.getRole().getRole().getRoleName() + "' is already assigned during this period",
                        "Adjust demand dates or choose different role/time period"
                    )
                );
            }
        }
    }
    
    private void validateSkillRequirements(Demand demand, DemandConflictValidationDTO validation) {
        // Check for excessive skill requirements
        if (demand.getRequiredSkills().size() > 15) {
            validation.getConflicts().add(
                DemandConflictValidationDTO.ConflictDetail.error(
                    "EXCESSIVE_SKILLS",
                    "Demand requires too many skills",
                    "Current skills: " + demand.getRequiredSkills().size() + " (Recommended maximum: 15)",
                    "Consolidate skill requirements or prioritize essential skills"
                )
            );
        } else if (demand.getRequiredSkills().size() > 10) {
            validation.getConflicts().add(
                DemandConflictValidationDTO.ConflictDetail.warning(
                    "MANY_SKILLS",
                    "Demand has many skill requirements",
                    "Current skills: " + demand.getRequiredSkills().size() + " (Recommended maximum: 10)",
                    "Consider if all skills are essential"
                )
            );
        }
        
        // Check for impossible skill combinations (simplified example)
        // This would need actual business logic based on your skill taxonomy
        boolean hasConflictingSkills = demand.getRequiredSkills().stream()
                .anyMatch(skill -> skill.getName().toLowerCase().contains("senior")) &&
                demand.getMinExp() < 5;
                
        if (hasConflictingSkills) {
            validation.getConflicts().add(
                DemandConflictValidationDTO.ConflictDetail.error(
                    "SKILL_EXPERIENCE_MISMATCH",
                    "Skill requirements conflict with experience level",
                    "Senior skills require minimum 5 years experience (current: " + demand.getMinExp() + ")",
                    "Increase minimum experience requirement or adjust skill level"
                )
            );
        }
    }
    
    private void validateBusinessRules(Demand demand, DemandConflictValidationDTO validation) {
        // Validate demand type specific rules
        if (demand.getDemandType() == DemandType.NET_NEW && 
            (demand.getDemandJustification() == null || demand.getDemandJustification().trim().length() < 20)) {
            validation.getConflicts().add(
                DemandConflictValidationDTO.ConflictDetail.error(
                    "INSUFFICIENT_JUSTIFICATION",
                    "Net-New demand requires detailed business justification",
                    "Justification must be at least 20 characters",
                    "Provide detailed business justification for this net-new demand"
                )
            );
        }
        
        if (demand.getDemandType() == DemandType.REPLACEMENT && demand.getOutgoingResource() == null) {
            validation.getConflicts().add(
                DemandConflictValidationDTO.ConflictDetail.error(
                    "MISSING_OUTGOING_RESOURCE",
                    "Replacement demand must specify outgoing resource",
                    "Replacement type requires outgoing resource reference",
                    "Specify the resource being replaced"
                )
            );
        }
        
        // Validate allocation percentage
        if (demand.getAllocationPercentage() < 10) {
            validation.getConflicts().add(
                DemandConflictValidationDTO.ConflictDetail.warning(
                    "LOW_ALLOCATION",
                    "Very low allocation percentage",
                    "Allocation: " + demand.getAllocationPercentage() + "% (Recommended minimum: 10%)",
                    "Consider if this low allocation is intentional"
                )
            );
        }
        
        if (demand.getAllocationPercentage() > 100) {
            validation.getConflicts().add(
                DemandConflictValidationDTO.ConflictDetail.error(
                    "INVALID_ALLOCATION",
                    "Allocation percentage exceeds 100%",
                    "Allocation: " + demand.getAllocationPercentage() + "% (Maximum: 100%)",
                    "Reduce allocation percentage to valid range (1-100)"
                )
            );
        }
    }

    @Override
    public ResponseEntity<ApiResponse<?>> getDemandKpiByResourceManagerId(Long resourceManagerId) {

        if (resourceManagerId == null) {
            throw new ProjectExceptionHandler(
                    HttpStatus.BAD_REQUEST,
                    "RESOURCE_MANAGER_ID_REQUIRED",
                    "Resource Manager ID is required"
            );
        }

        try {
            List<Demand> demands =
                    demandRepository.findByProjectResourceManagerId(resourceManagerId);

            DemandKpiDTO kpi = DemandKpiDTO.builder()
                    .active(0L)
                    .soft(0L)
                    .pending(0L)
                    .approved(0L)
                    .slaAtRisk(0L)
                    .slaBreached(0L)
                    .build();
            LocalDate today = LocalDate.now();

            for (Demand demand : demands) {

                DemandStatus status = demand.getDemandStatus();
                DemandCommitment commitment = demand.getDemandCommitment();

                if (status == null) continue;

                if (status == DemandStatus.DRAFT ||
                        status == DemandStatus.REJECTED ||
                        status == DemandStatus.CANCELLED ||
                        status == DemandStatus.FULFILLED) {
                    continue;
                }

                // -------- STATUS COUNTS --------
                switch (status) {
                    case REQUESTED -> kpi.setPending(kpi.getPending() + 1);
                    case APPROVED -> kpi.setApproved(kpi.getApproved() + 1);
                }

                // -------- ACTIVE --------
                if ((status == DemandStatus.REQUESTED || status == DemandStatus.APPROVED)
                        && commitment == DemandCommitment.CONFIRMED) {
                    kpi.setActive(kpi.getActive() + 1);
                }

                // -------- SOFT --------
                if (commitment == DemandCommitment.SOFT) {
                    kpi.setSoft(kpi.getSoft() + 1);
                }

                // -------- SLA --------
                demandSLARepository
                        .findByDemand_DemandIdAndActiveFlagTrue(demand.getDemandId())
                        .ifPresent(sla -> {

                            LocalDate dueAt = sla.getDueAt();
                            if (dueAt == null) return;

                            if (today.isAfter(dueAt)) {
                                kpi.setSlaBreached(kpi.getSlaBreached() + 1);
                                return;
                            }

                            Integer threshold = sla.getWarningThresholdDays();
                            if (threshold != null) {
                                long daysRemaining =
                                        ChronoUnit.DAYS.between(today, dueAt);

                                if (daysRemaining <= threshold) {
                                    kpi.setSlaAtRisk(kpi.getSlaAtRisk() + 1);
                                }
                            }
                        });
            }

            return ResponseEntity.ok(
                    ApiResponse.success("Demand KPI retrieved successfully", kpi)
            );

        } catch (ProjectExceptionHandler e) {
            return new ResponseEntity<>(ApiResponse.error(e.getMessage()), e.getStatus());
        } catch (Exception e) {
            return new ResponseEntity<>(
                    ApiResponse.error("Failed to retrieve demand KPI: " + e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    @Override
    public ResponseEntity<ApiResponse<?>> getDashboardKpi(UserDTO userDTO) {

        if (userDTO == null || userDTO.getId() == null) {
            throw new ProjectExceptionHandler(
                    HttpStatus.BAD_REQUEST,
                    "USER_REQUIRED",
                    "User information is required"
            );
        }

        try {
            List<Demand> demands =
                    demandRepository.findByProjectManagerIdOrCreatedBy(userDTO.getId());

            DashboardKpiDTO kpi = DashboardKpiDTO.builder()
                    .total(0L)
                    .active(0L)
                    .fulfilled(0L)
                    .soft(0L)
                    .pending(0L)
                    .approved(0L)
                    .build();

            for (Demand demand : demands) {

                DemandStatus status = demand.getDemandStatus();
                DemandCommitment commitment = demand.getDemandCommitment();

                if (status == null) continue;

                // Total (exclude cancelled & rejected if needed)
                if (status != DemandStatus.CANCELLED) {
                    kpi.setTotal(kpi.getTotal() + 1);
                }

                // Soft commitment
                if (commitment == DemandCommitment.SOFT) {
                    kpi.setSoft(kpi.getSoft() + 1);
                    continue; // soft should not be counted as active
                }

                switch (status) {
                    case REQUESTED -> {
                        kpi.setPending(kpi.getPending() + 1);
                        kpi.setActive(kpi.getActive() + 1);
                    }
                    case APPROVED -> {
                        kpi.setApproved(kpi.getApproved() + 1);
                        kpi.setActive(kpi.getActive() + 1);
                    }
                    case FULFILLED ->
                            kpi.setFulfilled(kpi.getFulfilled() + 1);
                    default -> {
                        // DRAFT, REJECTED, CANCELLED ignored
                    }
                }
            }

            return ResponseEntity.ok(
                    ApiResponse.success("Dashboard KPI retrieved successfully", kpi)
            );

        } catch (ProjectExceptionHandler e) {
            return new ResponseEntity<>(ApiResponse.error(e.getMessage()), e.getStatus());
        } catch (Exception e) {
            return new ResponseEntity<>(
                    ApiResponse.error("Failed to retrieve dashboard KPI: " + e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<?>> processDemandDecision(DemandDecisionDTO dto) {

        if (dto.getDemandId() == null) {
            throw new ProjectExceptionHandler(
                    HttpStatus.BAD_REQUEST,
                    "DEMAND_ID_REQUIRED",
                    "Demand ID is required"
            );
        }

        Demand demand = demandRepository.findById(dto.getDemandId())
                .orElseThrow(() -> new ProjectExceptionHandler(
                        HttpStatus.NOT_FOUND,
                        "DEMAND_NOT_FOUND",
                        "Demand not found"
                ));

        // Only REQUESTED demands can be approved/rejected
        if (demand.getDemandStatus() != DemandStatus.REQUESTED) {
            throw new ProjectExceptionHandler(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_STATE",
                    "Only REQUESTED demands can be approved or rejected"
            );
        }

        // APPROVE
        if (dto.getDecision() == DemandStatus.APPROVED) {

            demand.setDemandStatus(DemandStatus.APPROVED);
            demand.setRejectionReason(null);

        }

        // REJECT
        else if (dto.getDecision() == DemandStatus.REJECTED) {

            if (dto.getRejectionReason() == null || dto.getRejectionReason().isBlank()) {
                throw new ProjectExceptionHandler(
                        HttpStatus.BAD_REQUEST,
                        "REJECTION_REASON_REQUIRED",
                        "Rejection reason is mandatory"
                );
            }

            demand.setDemandStatus(DemandStatus.REJECTED);
            demand.setRejectionReason(dto.getRejectionReason());
        }

        else {
            throw new ProjectExceptionHandler(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_DECISION",
                    "Decision must be APPROVED or REJECTED"
            );
        }

        demandRepository.save(demand);

        return ResponseEntity.ok(
                ApiResponse.success("Demand decision processed successfully", demand.getDemandId())
        );
    }

    @Override
    public ResponseEntity<ApiResponse<DemandKpiDTO>> getDeliveryManagerKpi(UserDTO userDTO) {
        try {
            if (userDTO == null || userDTO.getId() == null) {
                throw new ProjectExceptionHandler(
                        HttpStatus.BAD_REQUEST,
                        "USER_REQUIRED",
                        "User information is required from authentication token"
                );
            }

            Long deliveryManagerId = userDTO.getId();

            // Get all projects for the delivery manager
            List<Project> projects = projectRepository.findByDeliveryOwnerId(deliveryManagerId);

            // Get all demands for these projects
            List<Demand> allDemands = new ArrayList<>();
            for (Project project : projects) {
                List<Demand> projectDemands = demandRepository.findByProject_PmsProjectId(project.getPmsProjectId());
                projectDemands.stream()
                        .filter(d -> d.getDemandStatus() != DemandStatus.DRAFT)
                        .filter(d -> d.getDemandStatus() != DemandStatus.CANCELLED)
                        .forEach(allDemands::add);
            }

            // Calculate KPI
            DemandKpiDTO kpi = calculateDemandKpi(allDemands);

            return ResponseEntity.ok(
                    ApiResponse.success("Delivery Manager KPI retrieved successfully", kpi)
            );

        } catch (ProjectExceptionHandler e) {
            return new ResponseEntity<>(ApiResponse.error(e.getMessage()), e.getStatus());
        } catch (Exception e) {
            return new ResponseEntity<>(
                    ApiResponse.error("Failed to retrieve delivery manager KPI: " + e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse<?>> processResourceManagerDecision(
            DemandDecisionDTO dto,
            UserDTO userDTO) {

        if (dto.getDemandId() == null) {
            throw new ProjectExceptionHandler(
                    HttpStatus.BAD_REQUEST,
                    "DEMAND_ID_REQUIRED",
                    "Demand ID is required"
            );
        }

        Demand demand = demandRepository.findById(dto.getDemandId())
                .orElseThrow(() -> new ProjectExceptionHandler(
                        HttpStatus.NOT_FOUND,
                        "DEMAND_NOT_FOUND",
                        "Demand not found"
                ));

        DemandStatus decision = dto.getDecision();

        if (decision == null) {
            throw new ProjectExceptionHandler(
                    HttpStatus.BAD_REQUEST,
                    "DECISION_REQUIRED",
                    "Decision is required"
            );
        }

        // RM can only act on APPROVED demands
        if (demand.getDemandStatus() != DemandStatus.APPROVED) {
            throw new ProjectExceptionHandler(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_STATE",
                    "Only APPROVED demands can be fulfilled or rejected by Resource Manager"
            );
        }

        // -------- FULFILLED --------
        if (decision == DemandStatus.FULFILLED) {

            demand.setDemandStatus(DemandStatus.FULFILLED);
            demand.setRejectionReason(null);

        }

        // -------- REJECTED --------
        else if (decision == DemandStatus.REJECTED) {

            if (dto.getRejectionReason() == null || dto.getRejectionReason().isBlank()) {
                throw new ProjectExceptionHandler(
                        HttpStatus.BAD_REQUEST,
                        "REJECTION_REASON_REQUIRED",
                        "Rejection reason is mandatory"
                );
            }

            demand.setDemandStatus(DemandStatus.REJECTED);
            demand.setRejectionReason(dto.getRejectionReason());
        }

        else {
            throw new ProjectExceptionHandler(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_DECISION",
                    "Resource Manager can only mark demand as FULFILLED or REJECTED"
            );
        }

        demandRepository.save(demand);

        return ResponseEntity.ok(
                ApiResponse.success("Demand status updated successfully", demand.getDemandId())
        );
    }

    @Override
    public ResponseEntity<ApiResponse<List<DeliveryManagerDemandDTO>>> getDeliveryManagerDemandDetails(UserDTO userDTO) {
        try {
            if (userDTO == null || userDTO.getId() == null) {
                throw new ProjectExceptionHandler(
                        HttpStatus.BAD_REQUEST,
                        "USER_REQUIRED",
                        "User information is required from authentication token"
                );
            }

            Long deliveryManagerId = userDTO.getId();

            // Get all projects for the delivery manager
            List<Project> projects = projectRepository.findByDeliveryOwnerId(deliveryManagerId);
            
            if (projects.isEmpty()) {
                return ResponseEntity.ok(
                        ApiResponse.success("No projects found for this delivery manager", new ArrayList<>())
                );
            }

            // Build flat list of all demands across all projects
            List<DeliveryManagerDemandDTO> allDemands = new ArrayList<>();
            LocalDate today = LocalDate.now();
            
            for (Project project : projects) {
                List<Demand> projectDemands = demandRepository.findByProject_PmsProjectId(project.getPmsProjectId());
                
                for (Demand demand : projectDemands) {
                    Optional<DemandSLA> demandSLAOpt = demandSLARepository.findByDemand_DemandIdAndActiveFlagTrue(demand.getDemandId());
                    
                    DeliveryManagerDemandDTO.DeliveryManagerDemandDTOBuilder demandBuilder = 
                            DeliveryManagerDemandDTO.builder()
                                    .clientId(project.getClientId())
                                    .clientName(project.getClient() != null ? project.getClient().getClientName() : "Unknown Client")
                                    .projectId(project.getPmsProjectId())
                                    .projectName(project.getName())
                                    .demandId(demand.getDemandId())
                                    .deliveryRole(demand.getRole().getRole().getRoleName())
                                    .demandJustification(demand.getDemandJustification())
                                    .minExp(demand.getMinExp())
                                    .resourceRequired(demand.getResourcesRequired())
                                    .allocation(demand.getAllocationPercentage())
                                    .demandName(demand.getDemandName())
                                    .demandPriority(demand.getDemandPriority() != null ? demand.getDemandPriority().toString() : "UNKNOWN")
                                    .priorityScore(calculatePriorityScore(demand))
                                    .demandStatus(demand.getDemandStatus() != null ? demand.getDemandStatus().toString() : "UNKNOWN")
                                    .demandType(demand.getDemandType() != null ? demand.getDemandType().toString() : "UNKNOWN")
                                    .deliveryModel(demand.getDeliveryModel() != null ? demand.getDeliveryModel().toString() : "UNKNOWN")
                                    .demandStartDate(demand.getDemandStartDate())
                                    .demandEndDate(demand.getDemandEndDate());
                    
                    // Add SLA details if present
                    if (demandSLAOpt.isPresent()) {
                        DemandSLA demandSLA = demandSLAOpt.get();
                        
                        demandBuilder
                                .demandSlaId(demandSLA.getDemandSlaId())
                                .slaType(demandSLA.getSlaType() != null ? demandSLA.getSlaType().toString() : "UNKNOWN")
                                .slaDurationDays(demandSLA.getSlaDurationDays())
                                .warningThresholdDays(demandSLA.getWarningThresholdDays())
                                .slaCreatedAt(demandSLA.getCreatedAt())
                                .slaDueAt(demandSLA.getDueAt())
                                .slaBreached(false)
                                .remainingDays(0)
                                .overdueDays(0);
                        
                        // Calculate SLA status
                        if (demandSLA.getDueAt() != null) {
                            if (today.isAfter(demandSLA.getDueAt())) {
                                demandBuilder
                                        .slaBreached(true)
                                        .overdueDays((int) ChronoUnit.DAYS.between(demandSLA.getDueAt(), today))
                                        .remainingDays(0);
                            } else {
                                demandBuilder
                                        .slaBreached(false)
                                        .remainingDays((int) ChronoUnit.DAYS.between(today, demandSLA.getDueAt()))
                                        .overdueDays(0);
                            }
                        }
                    } else {
                        // No SLA - set default values
                        demandBuilder
                                .demandSlaId(null)
                                .slaType(null)
                                .slaDurationDays(null)
                                .warningThresholdDays(null)
                                .slaCreatedAt(null)
                                .slaDueAt(null)
                                .slaBreached(false)
                                .remainingDays(0)
                                .overdueDays(0);
                    }
                    
                    allDemands.add(demandBuilder.build());
                }
            }

            return ResponseEntity.ok(
                    ApiResponse.success("Demands retrieved successfully", allDemands)
            );

        } catch (ProjectExceptionHandler e) {
            return new ResponseEntity<>(ApiResponse.error(e.getMessage()), e.getStatus());
        } catch (Exception e) {
            return new ResponseEntity<>(
                    ApiResponse.error("Failed to retrieve delivery manager demand details: " + e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    private DemandKpiDTO calculateDemandKpi(List<Demand> demands) {
        DemandKpiDTO kpi = DemandKpiDTO.builder()
                .active(0L)
                .soft(0L)
                .pending(0L)
                .approved(0L)
                .slaAtRisk(0L)
                .slaBreached(0L)
                .build();

        LocalDate today = LocalDate.now();

        for (Demand demand : demands) {
            DemandStatus status = demand.getDemandStatus();
            DemandCommitment commitment = demand.getDemandCommitment();

            if (status == null) continue;

            if (status == DemandStatus.DRAFT ||
                    status == DemandStatus.REJECTED ||
                    status == DemandStatus.CANCELLED ||
                    status == DemandStatus.FULFILLED) {
                continue;
            }


            // Status counts
            switch (status) {
                case REQUESTED -> kpi.setPending(kpi.getPending() + 1);
                case APPROVED -> kpi.setApproved(kpi.getApproved() + 1);
            }

            // Active demands
            if ((status == DemandStatus.REQUESTED || status == DemandStatus.APPROVED)
                    && commitment == DemandCommitment.CONFIRMED) {
                kpi.setActive(kpi.getActive() + 1);
            }

            // Soft commitments
            if (commitment == DemandCommitment.SOFT) {
                kpi.setSoft(kpi.getSoft() + 1);
            }

            // SLA calculations
            demandSLARepository
                    .findByDemand_DemandIdAndActiveFlagTrue(demand.getDemandId())
                    .ifPresent(sla -> {
                        LocalDate dueAt = sla.getDueAt();
                        if (dueAt == null) return;

                        if (today.isAfter(dueAt)) {
                            kpi.setSlaBreached(kpi.getSlaBreached() + 1);
                            return;
                        }

                        Integer threshold = sla.getWarningThresholdDays();
                        if (threshold != null) {
                            long daysRemaining = ChronoUnit.DAYS.between(today, dueAt);
                            if (daysRemaining <= threshold) {
                                kpi.setSlaAtRisk(kpi.getSlaAtRisk() + 1);
                            }
                        }
                    });
        }

        return kpi;
    }
}
