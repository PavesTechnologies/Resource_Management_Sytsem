package com.service_imple.project_service_impl;

import com.dto.ApiResponse;
import com.dto.project_dto.ProjectSLAResponseDTO;
import com.entity.client_entities.ClientSLA;
import com.entity.project_entities.Project;
import com.entity.project_entities.ProjectSLA;
import com.entity_enums.client_enums.SLAType;
import com.global_exception_handler.ProjectExceptionHandler;
import com.repo.client_repo.ClientSLARepo;
import com.repo.project_repo.ProjectRepository;
import com.repo.project_repo.ProjectSLARepo;
import com.service_interface.project_service_interface.ProjectSLAService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ProjectSLAServiceImpl implements ProjectSLAService {

    @Autowired
    private ProjectSLARepo projectSLARepo;
    
    @Autowired
    private ClientSLARepo clientSLARepo;
    
    @Autowired
    private ProjectRepository projectRepository;

    @Override
    public ResponseEntity<ApiResponse<ProjectSLAResponseDTO>> createOrUpdateProjectSLA(ProjectSLA projectSLA) {
        // Validate SLA values
        if (projectSLA.getSlaDurationDays() <= 0 || projectSLA.getWarningThresholdDays() <= 0) {
            throw ProjectExceptionHandler.badRequest("SLA duration and warning threshold must be greater than 0");
        }
        if (projectSLA.getWarningThresholdDays() >= projectSLA.getSlaDurationDays()) {
            throw ProjectExceptionHandler.badRequest("Warning threshold must be less than SLA duration");
        }
        
        // Check if this is an update or new entry
        ProjectSLA existingSLA = projectSLARepo.findByProject_PmsProjectIdAndSlaType(
            projectSLA.getProject().getPmsProjectId(),
            projectSLA.getSlaType()
        ).orElse(null);
        
        if (existingSLA != null) {
            // Update existing SLA
            existingSLA.setSlaDurationDays(projectSLA.getSlaDurationDays());
            existingSLA.setWarningThresholdDays(projectSLA.getWarningThresholdDays());
            existingSLA.setActiveFlag(projectSLA.getActiveFlag());
            existingSLA.setIsInherited(false); // Explicitly set to false as this is a custom override
            projectSLA = projectSLARepo.save(existingSLA);
        } else {
            // Create new SLA
            projectSLA.setIsInherited(false);
            projectSLA = projectSLARepo.save(projectSLA);
        }

        // Convert to response DTO
        ProjectSLAResponseDTO responseDTO = ProjectSLAResponseDTO.builder()
                .projectSlaId(projectSLA.getProjectSlaId())
                .projectId(projectSLA.getProject().getPmsProjectId())
                .slaType(projectSLA.getSlaType())
                .slaDurationDays(projectSLA.getSlaDurationDays())
                .warningThresholdDays(projectSLA.getWarningThresholdDays())
                .isInherited(projectSLA.getIsInherited())
                .activeFlag(projectSLA.getActiveFlag())
                .clientSlaId(projectSLA.getClientSLA() != null ? projectSLA.getClientSLA().getSlaId() : null)
                .build();

        return ResponseEntity.ok(ApiResponse.success("Project SLA saved successfully", responseDTO));
    }

    @Override
    public ResponseEntity<ApiResponse<ProjectSLAResponseDTO>> updateProjectSLA(UUID projectSlaId, ProjectSLA projectSLA) {
        // Find existing SLA
        ProjectSLA existingSLA = projectSLARepo.findById(projectSlaId)
            .orElseThrow(() -> ProjectExceptionHandler.notFound("Project SLA not found with id: " + projectSlaId));
        
        // Check if SLA is inherited - inherited SLAs cannot be updated
        if (existingSLA.getIsInherited()) {
            throw ProjectExceptionHandler.conflict("Cannot update inherited SLA. Only custom SLAs can be updated. To modify this SLA, create a custom override instead.");
        }
        
        // Validate SLA values
        if (projectSLA.getSlaDurationDays() <= 0 || projectSLA.getWarningThresholdDays() <= 0) {
            throw ProjectExceptionHandler.badRequest("SLA duration and warning threshold must be greater than 0");
        }
        if (projectSLA.getWarningThresholdDays() >= projectSLA.getSlaDurationDays()) {
            throw ProjectExceptionHandler.badRequest("Warning threshold must be less than SLA duration");
        }
        
        // Update only allowed fields
        existingSLA.setSlaDurationDays(projectSLA.getSlaDurationDays());
        existingSLA.setWarningThresholdDays(projectSLA.getWarningThresholdDays());
        existingSLA.setActiveFlag(projectSLA.getActiveFlag());
        existingSLA.setIsInherited(false); // Ensure it remains custom
        
        // Save the updated SLA
        ProjectSLA updatedSLA = projectSLARepo.save(existingSLA);
        
        // Convert to response DTO
        ProjectSLAResponseDTO responseDTO = ProjectSLAResponseDTO.builder()
                .projectSlaId(updatedSLA.getProjectSlaId())
                .projectId(updatedSLA.getProject().getPmsProjectId())
                .slaType(updatedSLA.getSlaType())
                .slaDurationDays(updatedSLA.getSlaDurationDays())
                .warningThresholdDays(updatedSLA.getWarningThresholdDays())
                .isInherited(updatedSLA.getIsInherited())
                .activeFlag(updatedSLA.getActiveFlag())
                .clientSlaId(updatedSLA.getClientSLA() != null ? updatedSLA.getClientSLA().getSlaId() : null)
                .build();
        
        return ResponseEntity.ok(ApiResponse.success(
            "Custom project SLA updated successfully", 
            responseDTO
        ));
    }

    @Override
    public ResponseEntity<ApiResponse<Void>> deleteProjectSLA(UUID projectSlaId) {

        ProjectSLA sla = projectSLARepo.findById(projectSlaId)
                .orElseThrow(() ->
                        ProjectExceptionHandler.notFound("Project SLA not found with id: " + projectSlaId));

        // Allow deletion of both inherited and custom SLAs
        projectSLARepo.delete(sla);

        return ResponseEntity.ok(
                new ApiResponse<Void>().getAPIResponse(
                        true,
                        "Project SLA deleted successfully",
                        null
                )
        );

    }

    @Override
    public ResponseEntity<ApiResponse<List<ProjectSLAResponseDTO>>> getProjectSLAByProjectId(Long projectId) {

        List<ProjectSLA> slas = projectSLARepo.findAllByProject_PmsProjectId(projectId)
                .orElseThrow(() ->
                        ProjectExceptionHandler.notFound("No SLAs found for project with id: " + projectId));

        List<ProjectSLAResponseDTO> dtoList = slas.stream().map(sla ->
                ProjectSLAResponseDTO.builder()
                        .projectSlaId(sla.getProjectSlaId())
                        .projectId(sla.getProject().getPmsProjectId())
                        .slaType(sla.getSlaType())
                        .slaDurationDays(sla.getSlaDurationDays())
                        .warningThresholdDays(sla.getWarningThresholdDays())
                        .isInherited(sla.getIsInherited())
                        .activeFlag(sla.getActiveFlag())
                        .clientSlaId(
                                sla.getClientSLA() != null ? sla.getClientSLA().getSlaId() : null
                        )
                        .build()
        ).toList();
        ApiResponse apiResponse=new ApiResponse<>();

        return ResponseEntity.ok(
                ApiResponse.success("Project SLAs retrieved successfully", dtoList)
        );
    }


    @Override
    public ResponseEntity<ApiResponse<ProjectSLAResponseDTO>> getProjectSLAByProjectAndType(Long projectId, SLAType slaType) {

        ProjectSLA sla = projectSLARepo
                .findByProject_PmsProjectIdAndSlaType(projectId, slaType)
                .orElseThrow(() ->
                        ProjectExceptionHandler.notFound(
                                "No " + slaType + " SLA found for project " + projectId));

        ProjectSLAResponseDTO dto = ProjectSLAResponseDTO.builder()
                .projectSlaId(sla.getProjectSlaId())
                .projectId(sla.getProject().getPmsProjectId())
                .slaType(sla.getSlaType())
                .slaDurationDays(sla.getSlaDurationDays())
                .warningThresholdDays(sla.getWarningThresholdDays())
                .isInherited(sla.getIsInherited())
                .activeFlag(sla.getActiveFlag())
                .clientSlaId(
                        sla.getClientSLA() != null ? sla.getClientSLA().getSlaId() : null
                )
                .build();
        ApiResponse apiResponse=new ApiResponse<>();

        return ResponseEntity.ok(
                apiResponse.getAPIResponse(true, "Project SLA retrieved successfully", dto)
        );
    }


    @Override
    public ResponseEntity<ApiResponse<ProjectSLAResponseDTO>> inheritClientSLA(Long projectId, SLAType slaType) {
        // Get the project to access client info
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> ProjectExceptionHandler.notFound("Project not found"));
        
        // Get the client SLA to inherit from
        ClientSLA clientSLA = clientSLARepo.findByClient_ClientIdAndSlaType(
            project.getClient().getClientId(), 
            slaType
        ).orElseThrow(() -> ProjectExceptionHandler.notFound(
            String.format("No %s SLA found for client: %s", 
                slaType, project.getClient().getClientName())));
        
        // Check if project SLA already exists
        ProjectSLA existingSLA = projectSLARepo.findByProject_PmsProjectIdAndSlaType(projectId, slaType).orElse(null);
        
        ProjectSLA projectSLA;
        if (existingSLA != null) {
            // Update existing SLA to inherit from client
            existingSLA.setClientSLA(clientSLA);
            existingSLA.setSlaDurationDays(clientSLA.getSlaDurationDays());
            existingSLA.setWarningThresholdDays(clientSLA.getWarningThresholdDays());
            existingSLA.setIsInherited(true);
            existingSLA.setActiveFlag(clientSLA.getActiveFlag());
            projectSLA = projectSLARepo.save(existingSLA);
        } else {
            // Create new inherited SLA
            projectSLA = ProjectSLA.builder()
                .project(project)
                .clientSLA(clientSLA)
                .slaType(slaType)
                .slaDurationDays(clientSLA.getSlaDurationDays())
                .warningThresholdDays(clientSLA.getWarningThresholdDays())
                .isInherited(true)
                .activeFlag(clientSLA.getActiveFlag())
                .build();
            projectSLA = projectSLARepo.save(projectSLA);
        }
        
        // Convert to response DTO
        ProjectSLAResponseDTO responseDTO = ProjectSLAResponseDTO.builder()
                .projectSlaId(projectSLA.getProjectSlaId())
                .projectId(projectSLA.getProject().getPmsProjectId())
                .slaType(projectSLA.getSlaType())
                .slaDurationDays(projectSLA.getSlaDurationDays())
                .warningThresholdDays(projectSLA.getWarningThresholdDays())
                .isInherited(projectSLA.getIsInherited())
                .activeFlag(projectSLA.getActiveFlag())
                .clientSlaId(projectSLA.getClientSLA() != null ? projectSLA.getClientSLA().getSlaId() : null)
                .build();
        
        return ResponseEntity.ok(new ApiResponse<ProjectSLAResponseDTO>().getAPIResponse(
            true, 
            "Successfully inherited client SLA for " + slaType.name(), 
            responseDTO
        ));
    }

    @Override
    public List<ProjectSLA> mapActiveClientSLAsToProject(Long projectId) {
        // Get the project to access client info
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> ProjectExceptionHandler.notFound("Project not found with id: " + projectId));
        
        // Get all active client SLAs for the project's client
        List<ClientSLA> activeClientSLAs = clientSLARepo.findByClient_ClientIdAndActiveFlag(
            project.getClient().getClientId(), 
            true
        );
        
        if (activeClientSLAs.isEmpty()) {
            return List.of(); // Return empty list if no active client SLAs found
        }
        
        // Create ProjectSLA entities for each active client SLA with inheritance
        List<ProjectSLA> inheritedSLAs = activeClientSLAs.stream().map(clientSLA -> {
            return ProjectSLA.builder()
                .project(project)
                .clientSLA(clientSLA)
                .slaType(clientSLA.getSlaType())
                .slaDurationDays(clientSLA.getSlaDurationDays())
                .warningThresholdDays(clientSLA.getWarningThresholdDays())
                .isInherited(true)
                .activeFlag(clientSLA.getActiveFlag())
                .build();
        }).toList();
        
        // Save and return all inherited SLAs
        return projectSLARepo.saveAll(inheritedSLAs);
    }

    @Override
    public ResponseEntity<ApiResponse<List<ProjectSLAResponseDTO>>> saveAll(List<ProjectSLA> projectSLAs) {
        if (projectSLAs == null || projectSLAs.isEmpty()) {
            throw ProjectExceptionHandler.badRequest("Project SLAs list cannot be null or empty");
        }

        List<ProjectSLAResponseDTO> responseDTOs = projectSLAs.stream().map(projectSLA -> {
            // Validate SLA values
            if (projectSLA.getSlaDurationDays() <= 0 || projectSLA.getWarningThresholdDays() <= 0) {
                throw ProjectExceptionHandler.badRequest("SLA duration and warning threshold must be greater than 0");
            }
            if (projectSLA.getWarningThresholdDays() >= projectSLA.getSlaDurationDays()) {
                throw ProjectExceptionHandler.badRequest("Warning threshold must be less than SLA duration");
            }

            // Check if this is an update or new entry
            ProjectSLA existingSLA = projectSLARepo.findByProject_PmsProjectIdAndSlaType(
                projectSLA.getProject().getPmsProjectId(),
                projectSLA.getSlaType()
            ).orElse(null);

            ProjectSLA savedSLA;
            if (existingSLA != null) {
                // Update existing SLA
                existingSLA.setSlaDurationDays(projectSLA.getSlaDurationDays());
                existingSLA.setWarningThresholdDays(projectSLA.getWarningThresholdDays());
                existingSLA.setActiveFlag(projectSLA.getActiveFlag());
                existingSLA.setIsInherited(false);
                savedSLA = projectSLARepo.save(existingSLA);
            } else {
                // Create new SLA
                projectSLA.setIsInherited(false);
                savedSLA = projectSLARepo.save(projectSLA);
            }

            // Convert to response DTO
            return ProjectSLAResponseDTO.builder()
                .projectSlaId(savedSLA.getProjectSlaId())
                .projectId(savedSLA.getProject().getPmsProjectId())
                .slaType(savedSLA.getSlaType())
                .slaDurationDays(savedSLA.getSlaDurationDays())
                .warningThresholdDays(savedSLA.getWarningThresholdDays())
                .isInherited(savedSLA.getIsInherited())
                .activeFlag(savedSLA.getActiveFlag())
                .clientSlaId(savedSLA.getClientSLA() != null ? savedSLA.getClientSLA().getSlaId() : null)
                .build();
        }).toList();

        return ResponseEntity.ok(new ApiResponse<List<ProjectSLAResponseDTO>>().getAPIResponse(
            true, 
            "Project SLAs saved successfully", 
            responseDTOs
        ));
    }
}
