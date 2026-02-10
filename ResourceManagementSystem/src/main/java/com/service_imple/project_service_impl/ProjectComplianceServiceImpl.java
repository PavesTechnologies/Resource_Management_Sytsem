package com.service_imple.project_service_impl;

import com.dto.ApiResponse;
import com.dto.project_dto.ProjectComplianceResponseDTO;
import com.entity.client_entities.ClientCompliance;
import com.entity.project_entities.Project;
import com.entity.project_entities.ProjectCompliance;
import com.entity_enums.client_enums.ComplianceType;
import com.global_exception_handler.ProjectExceptionHandler;
import com.repo.client_repo.ClientComplianceRepo;
import com.repo.project_repo.ProjectComplianceRepo;
import com.repo.project_repo.ProjectRepository;
import com.service_interface.project_service_interface.ProjectComplianceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ProjectComplianceServiceImpl implements ProjectComplianceService {

    @Autowired
    private ProjectComplianceRepo projectComplianceRepo;
    
    @Autowired
    private ClientComplianceRepo clientComplianceRepo;
    
    @Autowired
    private ProjectRepository projectRepository;

    @Override
    public ResponseEntity<ApiResponse<ProjectComplianceResponseDTO>> createOrUpdateProjectCompliance(ProjectCompliance projectCompliance) {
        // Validate compliance values
        if (projectCompliance.getRequirementName() == null || projectCompliance.getRequirementName().trim().isEmpty()) {
            throw ProjectExceptionHandler.badRequest("Requirement name cannot be empty");
        }
        
        // Check if this is an update or new entry
        ProjectCompliance existingCompliance = projectComplianceRepo.findByProject_PmsProjectIdAndComplianceType(
            projectCompliance.getProject().getPmsProjectId(),
            projectCompliance.getComplianceType()
        ).orElse(null);
        
        if (existingCompliance != null) {
            // Update existing compliance
            existingCompliance.setRequirementName(projectCompliance.getRequirementName());
            existingCompliance.setMandatoryFlag(projectCompliance.getMandatoryFlag());
            existingCompliance.setActiveFlag(projectCompliance.getActiveFlag());
            existingCompliance.setIsInherited(false); // Explicitly set to false as this is a custom override
            projectCompliance = projectComplianceRepo.save(existingCompliance);
        } else {
            // Create new compliance
            projectCompliance.setIsInherited(false);
            projectCompliance = projectComplianceRepo.save(projectCompliance);
        }

        // Convert to response DTO
        ProjectComplianceResponseDTO responseDTO = ProjectComplianceResponseDTO.builder()
                .projectComplianceId(projectCompliance.getProjectComplianceId())
                .projectId(projectCompliance.getProject().getPmsProjectId())
                .complianceType(projectCompliance.getComplianceType())
                .requirementName(projectCompliance.getRequirementName())
                .mandatoryFlag(projectCompliance.getMandatoryFlag())
                .isInherited(projectCompliance.getIsInherited())
                .activeFlag(projectCompliance.getActiveFlag())
                .clientComplianceId(projectCompliance.getClientCompliance() != null ? projectCompliance.getClientCompliance().getComplianceId() : null)
                .build();

        return ResponseEntity.ok(new ApiResponse<ProjectComplianceResponseDTO>().getAPIResponse(true, "Project compliance saved successfully", responseDTO));
    }

    @Override
    public ResponseEntity<ApiResponse<Void>> deleteProjectCompliance(UUID projectComplianceId) {
        ProjectCompliance compliance = projectComplianceRepo.findById(projectComplianceId)
            .orElseThrow(() -> ProjectExceptionHandler.notFound("Project compliance not found with id: " + projectComplianceId));
        
        if (compliance.getIsInherited()) {
            throw ProjectExceptionHandler.conflict("Cannot delete an inherited compliance. Please override it instead.");
        }
        
        projectComplianceRepo.delete(compliance);
        return ResponseEntity.ok(new ApiResponse<Void>().getAPIResponse(true, "Project compliance deleted successfully", null));
    }

    @Override
    public ResponseEntity<ApiResponse<List<ProjectComplianceResponseDTO>>> getProjectComplianceByProjectId(Long projectId) {

        List<ProjectCompliance> compliances = projectComplianceRepo.findAllByProject_PmsProjectId(projectId)
                .orElseThrow(() ->
                        ProjectExceptionHandler.notFound("No compliances found for project with id: " + projectId));

        List<ProjectComplianceResponseDTO> dtoList = compliances.stream().map(compliance ->
                ProjectComplianceResponseDTO.builder()
                        .projectComplianceId(compliance.getProjectComplianceId())
                        .projectId(compliance.getProject().getPmsProjectId())
                        .complianceType(compliance.getComplianceType())
                        .requirementName(compliance.getRequirementName())
                        .mandatoryFlag(compliance.getMandatoryFlag())
                        .isInherited(compliance.getIsInherited())
                        .activeFlag(compliance.getActiveFlag())
                        .clientComplianceId(
                                compliance.getClientCompliance() != null ? compliance.getClientCompliance().getComplianceId() : null
                        )
                        .build()
        ).toList();
        ApiResponse apiResponse=new ApiResponse<>();

        return ResponseEntity.ok(
                apiResponse.getAPIResponse(true, "Project compliances retrieved successfully", dtoList)
        );
    }


    @Override
    public ResponseEntity<ApiResponse<ProjectComplianceResponseDTO>> getProjectComplianceByProjectAndType(Long projectId, ComplianceType complianceType) {

        ProjectCompliance compliance = projectComplianceRepo
                .findByProject_PmsProjectIdAndComplianceType(projectId, complianceType)
                .orElseThrow(() ->
                        ProjectExceptionHandler.notFound(
                                "No " + complianceType + " compliance found for project " + projectId));

        ProjectComplianceResponseDTO dto = ProjectComplianceResponseDTO.builder()
                .projectComplianceId(compliance.getProjectComplianceId())
                .projectId(compliance.getProject().getPmsProjectId())
                .complianceType(compliance.getComplianceType())
                .requirementName(compliance.getRequirementName())
                .mandatoryFlag(compliance.getMandatoryFlag())
                .isInherited(compliance.getIsInherited())
                .activeFlag(compliance.getActiveFlag())
                .clientComplianceId(
                        compliance.getClientCompliance() != null ? compliance.getClientCompliance().getComplianceId() : null
                )
                .build();
        ApiResponse apiResponse=new ApiResponse<>();

        return ResponseEntity.ok(
                apiResponse.getAPIResponse(true, "Project compliance retrieved successfully", dto)
        );
    }


    @Override
    public ResponseEntity<ApiResponse<ProjectComplianceResponseDTO>> inheritClientCompliance(Long projectId, ComplianceType complianceType) {
        // Get the project to access client info
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> ProjectExceptionHandler.notFound("Project not found"));
        
        // Get the client compliance to inherit from
        ClientCompliance clientCompliance = clientComplianceRepo.findByClient_ClientIdAndRequirementType(
            project.getClient().getClientId(), 
            complianceType
        ).orElseThrow(() -> ProjectExceptionHandler.notFound(
            String.format("No %s compliance found for client: %s", 
                complianceType, project.getClient().getClientName())));
        
        // Check if project compliance already exists
        ProjectCompliance existingCompliance = projectComplianceRepo.findByProject_PmsProjectIdAndComplianceType(projectId, complianceType).orElse(null);
        
        ProjectCompliance projectCompliance;
        if (existingCompliance != null) {
            // Update existing compliance to inherit from client
            existingCompliance.setClientCompliance(clientCompliance);
            existingCompliance.setRequirementName(clientCompliance.getRequirementName());
            existingCompliance.setMandatoryFlag(clientCompliance.getMandatoryFlag());
            existingCompliance.setIsInherited(true);
            existingCompliance.setActiveFlag(clientCompliance.getActiveFlag());
            projectCompliance = projectComplianceRepo.save(existingCompliance);
        } else {
            // Create new inherited compliance
            projectCompliance = ProjectCompliance.builder()
                .project(project)
                .clientCompliance(clientCompliance)
                .complianceType(complianceType)
                .requirementName(clientCompliance.getRequirementName())
                .mandatoryFlag(clientCompliance.getMandatoryFlag())
                .isInherited(true)
                .activeFlag(clientCompliance.getActiveFlag())
                .build();
            projectCompliance = projectComplianceRepo.save(projectCompliance);
        }
        
        // Convert to response DTO
        ProjectComplianceResponseDTO responseDTO = ProjectComplianceResponseDTO.builder()
                .projectComplianceId(projectCompliance.getProjectComplianceId())
                .projectId(projectCompliance.getProject().getPmsProjectId())
                .complianceType(projectCompliance.getComplianceType())
                .requirementName(projectCompliance.getRequirementName())
                .mandatoryFlag(projectCompliance.getMandatoryFlag())
                .isInherited(projectCompliance.getIsInherited())
                .activeFlag(projectCompliance.getActiveFlag())
                .clientComplianceId(projectCompliance.getClientCompliance() != null ? projectCompliance.getClientCompliance().getComplianceId() : null)
                .build();
        
        return ResponseEntity.ok(new ApiResponse<ProjectComplianceResponseDTO>().getAPIResponse(
            true, 
            "Successfully inherited client compliance for " + complianceType.name(), 
            responseDTO
        ));
    }

    @Override
    public ResponseEntity<ApiResponse<List<ProjectComplianceResponseDTO>>> saveAll(List<ProjectCompliance> projectCompliances) {
        if (projectCompliances == null || projectCompliances.isEmpty()) {
            throw ProjectExceptionHandler.badRequest("Project compliances list cannot be null or empty");
        }

        List<ProjectComplianceResponseDTO> responseDTOs = projectCompliances.stream().map(projectCompliance -> {
            // Validate compliance values
            if (projectCompliance.getRequirementName() == null || projectCompliance.getRequirementName().trim().isEmpty()) {
                throw ProjectExceptionHandler.badRequest("Requirement name cannot be empty");
            }

            // Check if this is an update or new entry
            ProjectCompliance existingCompliance = projectComplianceRepo.findByProject_PmsProjectIdAndComplianceType(
                projectCompliance.getProject().getPmsProjectId(),
                projectCompliance.getComplianceType()
            ).orElse(null);

            ProjectCompliance savedCompliance;
            if (existingCompliance != null) {
                // Update existing compliance
                existingCompliance.setRequirementName(projectCompliance.getRequirementName());
                existingCompliance.setMandatoryFlag(projectCompliance.getMandatoryFlag());
                existingCompliance.setActiveFlag(projectCompliance.getActiveFlag());
                existingCompliance.setIsInherited(false);
                savedCompliance = projectComplianceRepo.save(existingCompliance);
            } else {
                // Create new compliance
                projectCompliance.setIsInherited(false);
                savedCompliance = projectComplianceRepo.save(projectCompliance);
            }

            // Convert to response DTO
            return ProjectComplianceResponseDTO.builder()
                .projectComplianceId(savedCompliance.getProjectComplianceId())
                .projectId(savedCompliance.getProject().getPmsProjectId())
                .complianceType(savedCompliance.getComplianceType())
                .requirementName(savedCompliance.getRequirementName())
                .mandatoryFlag(savedCompliance.getMandatoryFlag())
                .isInherited(savedCompliance.getIsInherited())
                .activeFlag(savedCompliance.getActiveFlag())
                .clientComplianceId(savedCompliance.getClientCompliance() != null ? savedCompliance.getClientCompliance().getComplianceId() : null)
                .build();
        }).toList();

        return ResponseEntity.ok(new ApiResponse<List<ProjectComplianceResponseDTO>>().getAPIResponse(
            true, 
            "Project compliances saved successfully", 
            responseDTOs
        ));
    }
}
