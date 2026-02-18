package com.service_imple.demand_service_impl;

import com.dto.ApiResponse;
import com.entity.demand_entities.Demand;
import com.entity.client_entities.ClientCompliance;
import com.entity_enums.client_enums.RequirementType;
//import com.entity_enums.skill_enums.DemandStatus;
import com.global_exception_handler.ProjectExceptionHandler;
import com.repo.DemandRepository;
import com.repo.client_repo.ClientComplianceRepo;
import com.service_imple.project_service_impl.ProjectDemandValidationService;
import com.service_interface.demand_service_interface.DemandService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DemandServiceImpl implements DemandService {

    @Autowired
    private DemandRepository demandRepository;

    @Autowired
    private ProjectDemandValidationService projectDemandValidationService;

    @Autowired
    private ClientComplianceRepo clientComplianceRepo;

    @Override
    public ResponseEntity<ApiResponse<?>> createDemand(Demand demand) {
        try {

            // 🔐 ADD THIS AS FIRST LINE (VERY IMPORTANT)
            projectDemandValidationService.validateProjectForStaffing(
                    demand.getProject().getPmsProjectId()
            );

            // 🔹 Auto-attach client compliance requirements
            List<ClientCompliance> compliances = clientComplianceRepo
                    .findAllByClient_ClientId(demand.getProject().getClient().getClientId())
                    .orElse(List.of());
            
            for (ClientCompliance compliance : compliances) {
                if (compliance.getActiveFlag() && compliance.getMandatoryFlag()) {
                    if (compliance.getRequirementType() == RequirementType.SKILL && compliance.getSkill() != null) {
                        demand.getRequiredSkills().add(compliance.getSkill());
                    } else if (compliance.getRequirementType() == RequirementType.CERTIFICATION && compliance.getCertificate() != null) {
                        demand.getRequiredCertificates().add(compliance.getCertificate());
                    }
                }
            }

            // Set default status if not provided
//            if (demand.getDemandStatus() == null) {
//                demand.setDemandStatus(
//                        com.entity_enums.skill_enums.DemandStatus.DRAFT
//                );
//            }

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

            ApiResponse response = ApiResponse.success(
                    "Demands retrieved successfully",
                    demands
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

}
