package com.service_imple.project_service_impl;

import com.config.ProjectDemandRules;
import com.entity.project_entities.Project;
import com.entity_enums.project_enums.ProjectStage;
import com.entity_enums.project_enums.ProjectStatus;
import com.global_exception_handler.ProjectExceptionHandler;
import com.repo.project_repo.ProjectRepository;
import com.service_interface.project_service_interface.ProjectGovernanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProjectDemandValidationService {
    private final ProjectRepository projectRepository;
    private final ProjectGovernanceService projectGovernanceService;

    /* ============================
       ENTRY POINT (USE EVERYWHERE)
       ============================ */
//    public Project validateProjectForStaffing(Long pmsProjectId) {
//
//        Project project = projectRepository.findById(pmsProjectId)
//                .orElseThrow(() -> new ProjectExceptionHandler(
//                        HttpStatus.NOT_FOUND,
//                        "PROJECT_NOT_FOUND",
//                        "Project does not exist in RMS"
//                ));
//
//        validateProjectStatus(project);     // STORY 7
//        validateLifecycleStage(project);    // STORY 8
//        validateGovernanceReadiness(pmsProjectId); // STORY 11
//
//        return project;
//    }

    /* ===============================
       STORY 7 — STATUS VALIDATION
       =============================== */
    private void validateProjectStatus(Project project) {

        ProjectStatus status = project.getProjectStatus();

        if (!ProjectDemandRules.ALLOWED_PROJECT_STATUSES.contains(status)) {
            throw new ProjectExceptionHandler(
                    HttpStatus.CONFLICT,
                    "INVALID_PROJECT_STATUS",
                    "Staffing blocked. Project status is " + status
            );
        }
    }

    /* ===============================
       STORY 8 — LIFECYCLE VALIDATION
       =============================== */
    private void validateLifecycleStage(Project project) {

        ProjectStage stage = project.getLifecycleStage();

        if (!ProjectDemandRules.ALLOWED_LIFECYCLE_STAGES.contains(stage)) {
            throw new ProjectExceptionHandler(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "INVALID_PROJECT_LIFECYCLE",
                    "Staffing not allowed in lifecycle stage " + stage
            );
        }
    }

    public void validateProjectForStaffing(Long projectId) {
    }

    /* ===============================
       STORY 11 — GOVERNANCE VALIDATION
       =============================== */
//    private void validateGovernanceReadiness(Long projectId) {
//        ApiResponse<ProjectGovernanceStatusDTO> response = projectGovernanceService.validateProjectGovernance(projectId);
//        ProjectGovernanceStatusDTO status = response.getData();
//
//        if (status == null ) {
//            throw new ProjectExceptionHandler(
//                    HttpStatus.UNPROCESSABLE_ENTITY,
//                    "INCOMPLETE_GOVERNANCE",
//                    status != null ? status.getMessage() : "Project governance validation failed"
//            );
//        }
//    }
}
