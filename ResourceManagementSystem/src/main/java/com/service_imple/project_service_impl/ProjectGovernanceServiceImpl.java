package com.service_imple.project_service_impl;

import com.dto.ApiResponse;
import com.dto.project_dto.*;
import com.entity.project_entities.Project;
import com.entity_enums.project_enums.ProjectDataStatus;
import com.global_exception_handler.ProjectExceptionHandler;
import com.repo.project_repo.ProjectComplianceRepo;
import com.repo.project_repo.ProjectEscalationRepo;
import com.repo.project_repo.ProjectRepository;
import com.repo.project_repo.ProjectSLARepo;
import com.service_interface.project_service_interface.ProjectGovernanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import com.config.ProjectDemandRules;


import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectGovernanceServiceImpl implements ProjectGovernanceService {

    private final ProjectRepository projectRepository;
    private final ProjectSLARepo projectSLARepo;
    private final ProjectEscalationRepo projectEscalationRepo;
    private final ProjectComplianceRepo projectComplianceRepo;

    // 🔹 STORY 9 — Task 2: Detect overlapping projects
    @Override
    public ApiResponse<List<ProjectOverlapDTO>> getProjectOverlaps(Long projectId) {

        Project current = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        List<ProjectOverlapDTO> overlaps = projectRepository.findOverlappingProjects(
                        current.getClientId(),
                        current.getStartDate(),
                        current.getEndDate(),
                        current.getPmsProjectId()
                ).stream()
                .map(p -> new ProjectOverlapDTO(
                        p.getPmsProjectId(),
                        p.getName(),
                        p.getStartDate(),
                        p.getEndDate()
                ))
                .toList();

        return new ApiResponse<>(true, "Overlapping projects fetched", overlaps);
    }

    // 🔹 STORY 9 — Task 3: Validate demand dates within project timeline
    @Override
    public ApiResponse<DateValidationResponse> validateDemandDates(
            Long projectId,
            DemandDateValidationRequest request) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        if (project.getStartDate() == null || project.getEndDate() == null) {
            return new ApiResponse<>(false,
                    "Project timeline is incomplete", null);
        }

        if (request.getDemandStart().isBefore(project.getStartDate())
                || request.getDemandEnd().isAfter(project.getEndDate())) {

            return new ApiResponse<>(false,
                    "Demand must fall within project start and end dates",
                    new DateValidationResponse(false, "Outside project timeline"));
        }

        return new ApiResponse<>(true,
                "Demand dates are valid",
                new DateValidationResponse(true, "Valid"));
    }

    // 🔹 STORY 10 — Task 1: Only eligible projects for demand
    @Override
    public ApiResponse<List<ProjectListDTO>> getEligibleProjects() {

        List<ProjectListDTO> eligibleProjects = projectRepository.findAll().stream()
                .map(this::mapWithEligibility)
                .filter(ProjectListDTO::eligibleForDemand)
                .toList();

        return new ApiResponse<>(true, "Eligible projects fetched", eligibleProjects);
    }
    
    @Override
    public ApiResponse<ProjectEligibilityDTO> checkProjectEligibility(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectExceptionHandler(HttpStatus.NOT_FOUND, "false", "Project not found"));

        ProjectListDTO eligibilityDetails = mapWithEligibility(project);
        
        ProjectEligibilityDTO result = new ProjectEligibilityDTO(
                eligibilityDetails.eligibleForDemand(),
                eligibilityDetails.visibilityReason()
        );

        return new ApiResponse<>(true, "Project eligibility status fetched", result);
    }

    // 🔹 STORY 10 — Task 3: Visibility across RMS
    @Override
    public ApiResponse<List<ProjectListDTO>> getAllProjectsWithVisibility() {

        List<ProjectListDTO> projects = projectRepository.findAll().stream()
                .map(this::mapWithEligibility)
                .toList();

        return new ApiResponse<>(true, "Project visibility list fetched", projects);
    }

    @Override
    public ApiResponse<List<Project>> getProjectsByManagerId(Long managerId) {
        List<Project> projects = projectRepository.findAllByresourceManagerId(managerId)
                .orElseThrow(() -> new ProjectExceptionHandler(HttpStatus.NOT_FOUND, "false", "No projects found for this Manager"));

        for (Project project : projects) {
            if (project.getStartDate() != null && project.getEndDate() != null) {
                List<Project> overlaps = projectRepository.findOverlappingProjects(
                        project.getClientId(),
                        project.getStartDate(),
                        project.getEndDate(),
                        project.getPmsProjectId()
                );
                project.setHasOverlap(!overlaps.isEmpty());
            }
        }

        return new ApiResponse<>(true, "Projects fetched", projects);
    }

    // 🔹 STORY 11 — Task 1: Validate Project Governance Completeness
    @Override
    public ApiResponse<ProjectGovernanceStatusDTO> validateProjectGovernance(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new ProjectExceptionHandler(HttpStatus.NOT_FOUND, "false", "Project not found");
        }

        boolean slaComplete = projectSLARepo.existsByProject_PmsProjectId(projectId);
        boolean escalationComplete = projectEscalationRepo.existsByProject_PmsProjectId(projectId);
        boolean complianceComplete = projectComplianceRepo.existsByProject_PmsProjectId(projectId);

        boolean isReady = slaComplete && escalationComplete && complianceComplete;
        String message = isReady ? "Project is ready for demand creation" : "Project governance configuration is incomplete";

        ProjectGovernanceStatusDTO statusDTO = ProjectGovernanceStatusDTO.builder()
                .projectId(projectId)
                .slaComplete(slaComplete)
                .escalationComplete(escalationComplete)
                .complianceComplete(complianceComplete)
                .isReadyForDemand(isReady)
                .message(message)
                .build();

        return new ApiResponse<>(true, "Project governance status fetched", statusDTO);
    }

    // 🔹 Central eligibility + visibility logic
    private ProjectListDTO mapWithEligibility(Project project) {

        boolean eligible = true;
        String reason = null;

        if (project.getDataStatus() != ProjectDataStatus.COMPLETE) {
            eligible = false;
            reason = "Incomplete PMS data";
        }
        else if (!ProjectDemandRules.ALLOWED_PROJECT_STATUSES.contains(project.getProjectStatus())) {
            eligible = false;
            reason = "Project not in active/approved state";
        }
        else if (!ProjectDemandRules.ALLOWED_LIFECYCLE_STAGES.contains(project.getLifecycleStage())) {
            eligible = false;
            reason = "Project not in staffing lifecycle stage";
        }


        return new ProjectListDTO(
                project.getPmsProjectId(),
                project.getName(),
                project.getProjectStatus(),
                project.getLifecycleStage(),
                eligible,
                reason
        );
    }
}
