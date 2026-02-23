package com.service_imple.demand_service_impl;

import com.dto.ApiResponse;
import com.dto.skill_dto.DeliveryRoleExpectationResponse;
import com.entity.demand_entities.Demand;
import com.entity.project_entities.ProjectCompliance;
import com.entity_enums.client_enums.RequirementType;
//import com.entity_enums.skill_enums.DemandStatus;
import com.global_exception_handler.ProjectExceptionHandler;
import com.repo.DemandRepository;
import com.repo.project_repo.ProjectComplianceRepo;
import com.service_imple.project_service_impl.ProjectDemandValidationService;
import com.service_interface.demand_service_interface.DemandService;
import com.service_interface.skill_service_interface.DeliveryRoleExpectationService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class DemandServiceImpl implements DemandService {

    @Autowired
    private DemandRepository demandRepository;

    @Autowired
    private ProjectDemandValidationService projectDemandValidationService;

    @Autowired
    private ProjectComplianceRepo projectComplianceRepo;

    @Autowired
    private DeliveryRoleExpectationService deliveryRoleExpectationService;

    @Override
    public ResponseEntity<ApiResponse<?>> createDemand(Demand demand) {
        try {

            // 🔐 ADD THIS AS FIRST LINE (VERY IMPORTANT)
            projectDemandValidationService.validateProjectForStaffing(
                    demand.getProject().getPmsProjectId()
            );

            // 🔹 Auto-attach project compliance requirements
            List<ProjectCompliance> compliances = projectComplianceRepo
                    .findAllByProject_PmsProjectId(demand.getProject().getPmsProjectId())
                    .orElse(List.of());

            for (ProjectCompliance compliance : compliances) {
                if (compliance.getActiveFlag() && compliance.getMandatoryFlag()) {
                    if (compliance.getRequirementType() == RequirementType.SKILL && compliance.getClientCompliance() != null && compliance.getClientCompliance().getSkill() != null) {
                        demand.getRequiredSkills().add(compliance.getClientCompliance().getSkill());
                    } else if (compliance.getRequirementType() == RequirementType.CERTIFICATION && compliance.getClientCompliance() != null && compliance.getClientCompliance().getCertificate() != null) {
                        demand.getRequiredCertificates().add(compliance.getClientCompliance().getCertificate());
                    }
                }
            }

            // Set created timestamp
            demand.setCreatedAt(LocalDateTime.now());

            // Save the demand
            Demand savedDemand = demandRepository.save(demand);

            ApiResponse response = ApiResponse.success(
                    "Demand created successfully",
                    savedDemand.getDemandId()
            );

            return new ResponseEntity<>(response, HttpStatus.CREATED);

        } catch (ProjectExceptionHandler e) {
            // ✅ Business validation failure (expected)
            ApiResponse response = ApiResponse.error(
                    e.getMessage()
            );
            return new ResponseEntity<>(response, e.getStatus());

        } catch (Exception e) {
            // ❌ Unexpected failure
            ApiResponse response = ApiResponse.error(
                    "Failed to create demand: " + e.getMessage()
            );
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    @Override
    public ResponseEntity<ApiResponse<?>> updateDemand(Demand request) {

        if (request.getDemandId() == null) {
            throw new ProjectExceptionHandler(
                    HttpStatus.BAD_REQUEST,
                    "DEMAND_ID_REQUIRED",
                    "Demand ID is required for update"
            );
        }

        Demand existing = demandRepository.findById(request.getDemandId())
                .orElseThrow(() -> new ProjectExceptionHandler(
                        HttpStatus.NOT_FOUND,
                        "DEMAND_NOT_FOUND",
                        "Demand not found"
                ));

//        if (existing.getDemandStatus() != DemandStatus.DRAFT) {
//            throw new ProjectExceptionHandler(
//                    HttpStatus.CONFLICT,
//                    "INVALID_DEMAND_STATUS",
//                    "Only DRAFT demands can be updated"
//            );
//        }

        projectDemandValidationService.validateProjectForStaffing(
                existing.getProject().getPmsProjectId()
        );

        if (request.getDemandEndDate() != null &&
                request.getDemandStartDate() != null &&
                request.getDemandEndDate().isBefore(request.getDemandStartDate())) {

            throw new ProjectExceptionHandler(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_DATE_RANGE",
                    "Demand end date cannot be before start date"
            );
        }

        // SAFE updates
        if (request.getDemandJustification() != null)
            existing.setDemandJustification(request.getDemandJustification());
        if (request.getDemandStartDate() != null)
            existing.setDemandStartDate(request.getDemandStartDate());
        if (request.getDemandEndDate() != null)
            existing.setDemandEndDate(request.getDemandEndDate());
        if (request.getAllocationPercentage() != null)
            existing.setAllocationPercentage(request.getAllocationPercentage());
        if (request.getDeliveryModel() != null)
            existing.setDeliveryModel(request.getDeliveryModel());
        if (request.getLocationRequirement() != null)
            existing.setLocationRequirement(request.getLocationRequirement());
        if (request.getDemandPriority() != null)
            existing.setDemandPriority(request.getDemandPriority());
        if (request.getDemandType() != null)
            existing.setDemandType(request.getDemandType());

        demandRepository.save(existing);

        return ResponseEntity.ok(
                ApiResponse.success("Demand updated successfully", existing.getDemandId())
        );
    }


    @Override
    public ResponseEntity<ApiResponse<?>> getDemandByProjectId(Long projectId) {
        try {
            // Validate project ID
            if (projectId == null) {
                throw new ProjectExceptionHandler(
                        HttpStatus.BAD_REQUEST,
                        "PROJECT_ID_REQUIRED",
                        "Project ID is required"
                );
            }

            // Validate project exists and is eligible for staffing
            projectDemandValidationService.validateProjectForStaffing(projectId);

            // Fetch demands by project ID
            List<Demand> demands = demandRepository.findByProject_PmsProjectId(projectId);

            // Extract only demand IDs
            List<UUID> demandIds = demands.stream()
                    .map(Demand::getDemandId)
                    .collect(java.util.stream.Collectors.toList());

            ApiResponse response = ApiResponse.success(
                    "Demand IDs retrieved successfully",
                    demandIds
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
                    "Failed to retrieve demand IDs: " + e.getMessage()
            );
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<ApiResponse<?>> getDemandById(UUID demandId) {
        try {
            // Validate demand ID
            if (demandId == null) {
                throw new ProjectExceptionHandler(
                        HttpStatus.BAD_REQUEST,
                        "DEMAND_ID_REQUIRED",
                        "Demand ID is required"
                );
            }

            // Fetch demand by ID
            Demand demand = demandRepository.findById(demandId)
                    .orElseThrow(() -> new ProjectExceptionHandler(
                            HttpStatus.NOT_FOUND,
                            "DEMAND_NOT_FOUND",
                            "Demand not found"
                    ));

            // Format the demand response with proper role data
            java.util.Map<String, Object> formattedDemand = new java.util.HashMap<>();
            formattedDemand.put("demandId", demand.getDemandId());
            formattedDemand.put("demandJustification", demand.getDemandJustification());
            formattedDemand.put("demandStartDate", demand.getDemandStartDate());
            formattedDemand.put("demandEndDate", demand.getDemandEndDate());
            formattedDemand.put("allocationPercentage", demand.getAllocationPercentage());
            formattedDemand.put("locationRequirement", demand.getLocationRequirement());
            formattedDemand.put("deliveryModel", demand.getDeliveryModel());
            formattedDemand.put("demandType", demand.getDemandType());
            formattedDemand.put("demandStatus", demand.getDemandStatus());
            formattedDemand.put("demandPriority", demand.getDemandPriority());
            formattedDemand.put("createdBy", demand.getCreatedBy());
            formattedDemand.put("createdAt", demand.getCreatedAt());

            // Get properly formatted role data
            DeliveryRoleExpectationResponse roleResponse = null;
            if (demand.getRole() != null && demand.getRole().getRoleName() != null) {
                roleResponse = deliveryRoleExpectationService.getRoleExpectations(demand.getRole().getRoleName());
            }
            formattedDemand.put("role", roleResponse);

            // Project data (avoid proxy issues)
            if (demand.getProject() != null) {
                java.util.Map<String, Object> projectMap = new java.util.HashMap<>();
                projectMap.put("pmsProjectId", demand.getProject().getPmsProjectId());
                projectMap.put("name", demand.getProject().getName());
                projectMap.put("clientId", demand.getProject().getClientId());
                projectMap.put("projectManagerId", demand.getProject().getProjectManagerId());
                projectMap.put("resourceManagerId", demand.getProject().getResourceManagerId());
                projectMap.put("deliveryOwnerId", demand.getProject().getDeliveryOwnerId());
                projectMap.put("deliveryModel", demand.getProject().getDeliveryModel());
                projectMap.put("primaryLocation", demand.getProject().getPrimaryLocation());
                projectMap.put("riskLevel", demand.getProject().getRiskLevel());
                projectMap.put("riskLevelUpdatedAt", demand.getProject().getRiskLevelUpdatedAt());
                projectMap.put("priorityLevel", demand.getProject().getPriorityLevel());
                projectMap.put("startDate", demand.getProject().getStartDate());
                projectMap.put("endDate", demand.getProject().getEndDate());
                projectMap.put("projectBudget", demand.getProject().getProjectBudget());
                projectMap.put("projectBudgetCurrency", demand.getProject().getProjectBudgetCurrency());
                projectMap.put("projectStatus", demand.getProject().getProjectStatus());
                projectMap.put("lifecycleStage", demand.getProject().getLifecycleStage());
                projectMap.put("dataStatus", demand.getProject().getDataStatus());
                projectMap.put("staffingReadinessStatus", demand.getProject().getStaffingReadinessStatus());
                projectMap.put("staffingReadinessReason", demand.getProject().getStaffingReadinessReason());
                projectMap.put("staffingReadinessUpdatedAt", demand.getProject().getStaffingReadinessUpdatedAt());
                projectMap.put("createdAt", demand.getProject().getCreatedAt());
                projectMap.put("hasOverlap", demand.getProject().getHasOverlap());
                projectMap.put("lastSyncedAt", demand.getProject().getLastSyncedAt());
                formattedDemand.put("project", projectMap);
            }

            // Collections (avoid proxy issues)
            formattedDemand.put("requiredSkills", demand.getRequiredSkills().stream()
                    .map(skill -> skill.getId())
                    .collect(java.util.stream.Collectors.toList()));

            formattedDemand.put("requiredCertificates", demand.getRequiredCertificates().stream()
                    .map(cert -> cert.getCertificateId())
                    .collect(java.util.stream.Collectors.toList()));

            ApiResponse response = ApiResponse.success(
                    "Demand retrieved successfully",
                    formattedDemand
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
                    "Failed to retrieve demand: " + e.getMessage()
            );
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
