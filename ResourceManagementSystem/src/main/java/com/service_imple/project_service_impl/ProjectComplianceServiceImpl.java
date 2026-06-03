package com.service_imple.project_service_impl;

import com.dto.centralised_dto.ApiResponse;
import com.dto.project_dto.ProjectComplianceResponseDTO;
import com.entity.client_entities.ClientCompliance;
import com.entity.project_entities.Project;
import com.entity.project_entities.ProjectCompliance;
import com.entity_enums.client_enums.RequirementType;
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

        if (projectCompliance.getRequirementName() == null ||
                projectCompliance.getRequirementName().trim().isEmpty()) {
            throw ProjectExceptionHandler.badRequest("Requirement name cannot be empty");
        }

        ProjectCompliance savedCompliance;

        // ✅ If ID exists → update
        if (projectCompliance.getProjectComplianceId() != null) {

            ProjectCompliance existing = projectComplianceRepo.findById(projectCompliance.getProjectComplianceId())
                    .orElseThrow(() -> ProjectExceptionHandler.notFound("Project compliance not found"));

            existing.setRequirementName(projectCompliance.getRequirementName());
            existing.setMandatoryFlag(projectCompliance.getMandatoryFlag());
            existing.setActiveFlag(projectCompliance.getActiveFlag());

            savedCompliance = projectComplianceRepo.save(existing);

        } else {
            // ✅ ALWAYS create new record
            projectCompliance.setIsInherited(false);
            projectCompliance.setClientCompliance(null); // important
            savedCompliance = projectComplianceRepo.save(projectCompliance);
        }

        ProjectComplianceResponseDTO responseDTO = ProjectComplianceResponseDTO.builder()
                .projectComplianceId(savedCompliance.getProjectComplianceId())
                .projectId(savedCompliance.getProject().getPmsProjectId())
                .requirementType(savedCompliance.getRequirementType())
                .requirementName(savedCompliance.getRequirementName())
                .mandatoryFlag(savedCompliance.getMandatoryFlag())
                .isInherited(savedCompliance.getIsInherited())
                .activeFlag(savedCompliance.getActiveFlag())
                .clientComplianceId(savedCompliance.getClientCompliance() != null ?
                        savedCompliance.getClientCompliance().getComplianceId() : null)
                .build();

        return ResponseEntity.ok(
                ApiResponse.success("Project compliance saved successfully", responseDTO)
        );
    }

    @Override
    public ResponseEntity<ApiResponse<ProjectComplianceResponseDTO>> updateProjectCompliance(UUID projectComplianceId, ProjectCompliance projectCompliance) {
        // Find existing compliance
        ProjectCompliance existingCompliance = projectComplianceRepo.findById(projectComplianceId)
            .orElseThrow(() -> ProjectExceptionHandler.notFound("Project compliance not found with id: " + projectComplianceId));
        
        // Check if compliance is inherited - inherited compliances cannot be updated
        if (existingCompliance.getIsInherited()) {
            throw ProjectExceptionHandler.conflict("Cannot update inherited compliance. Only custom compliances can be updated. To modify this compliance, create a custom override instead.");
        }
        
        // Validate compliance values
        if (projectCompliance.getRequirementName() == null || projectCompliance.getRequirementName().trim().isEmpty()) {
            throw ProjectExceptionHandler.badRequest("Requirement name cannot be empty");
        }
        
        // Update only allowed fields
        existingCompliance.setRequirementName(projectCompliance.getRequirementName());
        existingCompliance.setMandatoryFlag(projectCompliance.getMandatoryFlag());
        existingCompliance.setActiveFlag(projectCompliance.getActiveFlag());
        existingCompliance.setIsInherited(false); // Ensure it remains custom
        
        // Save the updated compliance
        ProjectCompliance updatedCompliance = projectComplianceRepo.save(existingCompliance);
        
        // Convert to response DTO
        ProjectComplianceResponseDTO responseDTO = ProjectComplianceResponseDTO.builder()
                .projectComplianceId(updatedCompliance.getProjectComplianceId())
                .projectId(updatedCompliance.getProject().getPmsProjectId())
                .requirementType(updatedCompliance.getRequirementType())
                .requirementName(updatedCompliance.getRequirementName())
                .mandatoryFlag(updatedCompliance.getMandatoryFlag())
                .isInherited(updatedCompliance.getIsInherited())
                .activeFlag(updatedCompliance.getActiveFlag())
                .clientComplianceId(updatedCompliance.getClientCompliance() != null ? updatedCompliance.getClientCompliance().getComplianceId() : null)
                .build();
        
        return ResponseEntity.ok(ApiResponse.success(
            "Custom project compliance updated successfully", 
            responseDTO
        ));
    }

    @Override
    public ResponseEntity<ApiResponse<Void>> deleteProjectCompliance(UUID projectComplianceId) {

        ProjectCompliance compliance = projectComplianceRepo.findById(projectComplianceId)
                .orElseThrow(() ->
                        ProjectExceptionHandler.notFound("Project compliance not found with id: " + projectComplianceId)
                );

        // 🔥 REMOVE inherited restriction
        projectComplianceRepo.delete(compliance);

        return ResponseEntity.ok(
                ApiResponse.success("Project compliance removed successfully", null)
        );
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
                        .requirementType(compliance.getRequirementType())
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
                ApiResponse.success("Project compliances retrieved successfully", dtoList)
        );
    }


    @Override
    public ResponseEntity<ApiResponse<ProjectComplianceResponseDTO>> getProjectComplianceByProjectAndType(Long projectId, RequirementType requirementType) {

        ProjectCompliance compliance = projectComplianceRepo
                .findByProject_PmsProjectIdAndRequirementType(projectId, requirementType)
                .orElseThrow(() ->
                        ProjectExceptionHandler.notFound(
                                "No " + requirementType + " compliance found for project " + projectId));

        ProjectComplianceResponseDTO dto = ProjectComplianceResponseDTO.builder()
                .projectComplianceId(compliance.getProjectComplianceId())
                .projectId(compliance.getProject().getPmsProjectId())
                .requirementType(compliance.getRequirementType())
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
                ApiResponse.success("Project compliance retrieved successfully", dto)
        );
    }


    @Override
    public ResponseEntity<ApiResponse<ProjectComplianceResponseDTO>> inheritClientCompliance(
            Long projectId, RequirementType requirementType) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> ProjectExceptionHandler.notFound("Project not found"));

        ClientCompliance clientCompliance =
                clientComplianceRepo.findByClient_ClientIdAndRequirementType(
                        project.getClient().getClientId(),
                        requirementType
                ).orElseThrow(() ->
                        ProjectExceptionHandler.notFound(
                                "No " + requirementType + " compliance found for client"
                        ));

        // 🔥 ONLY check if already inherited
        boolean alreadyInherited = projectComplianceRepo
                .existsByProject_PmsProjectIdAndRequirementTypeAndIsInheritedTrue(
                        projectId, requirementType
                );

        if (alreadyInherited) {
            throw ProjectExceptionHandler.conflict(
                    "Compliance already inherited for this project."
            );
        }

        // 🔥 ALWAYS create new inherited record
        ProjectCompliance projectCompliance = ProjectCompliance.builder()
                .project(project)
                .clientCompliance(clientCompliance)
                .requirementType(requirementType)
                .requirementName(clientCompliance.getRequirementName())
                .mandatoryFlag(clientCompliance.getMandatoryFlag())
                .isInherited(true)
                .activeFlag(clientCompliance.getActiveFlag())
                .build();

        projectCompliance = projectComplianceRepo.save(projectCompliance);

        ProjectComplianceResponseDTO responseDTO = ProjectComplianceResponseDTO.builder()
                .projectComplianceId(projectCompliance.getProjectComplianceId())
                .projectId(projectCompliance.getProject().getPmsProjectId())
                .requirementType(projectCompliance.getRequirementType())
                .requirementName(projectCompliance.getRequirementName())
                .mandatoryFlag(projectCompliance.getMandatoryFlag())
                .isInherited(projectCompliance.getIsInherited())
                .activeFlag(projectCompliance.getActiveFlag())
                .clientComplianceId(clientCompliance.getComplianceId())
                .build();

        return ResponseEntity.ok(
                ApiResponse.success("Successfully inherited client compliance", responseDTO)
        );
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
            ProjectCompliance existingCompliance = projectComplianceRepo.findByProject_PmsProjectIdAndRequirementType(
                projectCompliance.getProject().getPmsProjectId(),
                projectCompliance.getRequirementType()
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
                .requirementType(savedCompliance.getRequirementType())
                .requirementName(savedCompliance.getRequirementName())
                .mandatoryFlag(savedCompliance.getMandatoryFlag())
                .isInherited(savedCompliance.getIsInherited())
                .activeFlag(savedCompliance.getActiveFlag())
                .clientComplianceId(savedCompliance.getClientCompliance() != null ? savedCompliance.getClientCompliance().getComplianceId() : null)
                .build();
        }).toList();

        return ResponseEntity.ok(ApiResponse.success(
            "Project compliances saved successfully", 
            responseDTOs
        ));
    }
}
