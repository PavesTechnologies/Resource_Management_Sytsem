package com.service_imple.project_service_impl;

import com.dto.ApiResponse;
import com.dto.project_dto.DateValidationResponse;
import com.dto.project_dto.DemandDateValidationRequest;
import com.dto.project_dto.ProjectListDTO;
import com.dto.project_dto.ProjectOverlapDTO;
import com.entity.project_entities.Project;
import com.entity_enums.project_enums.ProjectStatus;
import com.entity_enums.project_enums.ProjectStage;
import com.entity_enums.project_enums.ProjectDataStatus;
import com.global_exception_handler.ProjectExceptionHandler;
import com.repo.project_repo.ProjectRepository;
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

        return new ApiResponse<>(true, "Projects fetched", projectRepository.findAllByresourceManagerId(managerId).orElseThrow(()-> new ProjectExceptionHandler(HttpStatus.NOT_FOUND,"false","No projects found for this Manager")));
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
