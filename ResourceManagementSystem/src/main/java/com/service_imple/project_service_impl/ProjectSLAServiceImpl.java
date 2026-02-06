package com.service_imple.project_service_impl;

import com.dto.ApiResponse;
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
    
    @Autowired
    private ApiResponse apiResponse;

    @Override
    public ResponseEntity<ApiResponse> createOrUpdateProjectSLA(ProjectSLA projectSLA) {
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
        
        return ResponseEntity.ok(apiResponse.getAPIResponse(true, "Project SLA saved successfully", projectSLA));
    }

    @Override
    public ResponseEntity<ApiResponse> deleteProjectSLA(UUID projectSlaId) {
        ProjectSLA sla = projectSLARepo.findById(projectSlaId)
            .orElseThrow(() -> ProjectExceptionHandler.notFound("Project SLA not found with id: " + projectSlaId));
        
        if (sla.getIsInherited()) {
            throw ProjectExceptionHandler.conflict("Cannot delete an inherited SLA. Please override it instead.");
        }
        
        projectSLARepo.delete(sla);
        return ResponseEntity.ok(apiResponse.getAPIResponse(true, "Project SLA deleted successfully", null));
    }

    @Override
    public ResponseEntity<ApiResponse> getProjectSLAByProjectId(UUID projectId) {
        List<ProjectSLA> slas = projectSLARepo.findAllByProject_ProjectId(projectId)
            .orElseThrow(() -> ProjectExceptionHandler.notFound("No SLAs found for project with id: " + projectId));
        return ResponseEntity.ok(apiResponse.getAPIResponse(true, "Project SLAs retrieved successfully", slas));
    }

    @Override
    public ResponseEntity<ApiResponse> getProjectSLAByProjectAndType(Long projectId, SLAType slaType) {
        ProjectSLA sla = projectSLARepo.findByProject_PmsProjectIdAndSlaType(projectId, slaType)
            .orElseThrow(() -> ProjectExceptionHandler.notFound(
                String.format("No %s SLA found for project with id: %s", slaType, projectId)));
        return ResponseEntity.ok(apiResponse.getAPIResponse(true, "Project SLA retrieved successfully", sla));
    }

    @Override
    public ResponseEntity<ApiResponse> inheritClientSLA(Long projectId, SLAType slaType) {
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
        
        return ResponseEntity.ok(apiResponse.getAPIResponse(
            true, 
            "Successfully inherited client SLA for " + slaType.name(), 
            projectSLA
        ));
    }
}
