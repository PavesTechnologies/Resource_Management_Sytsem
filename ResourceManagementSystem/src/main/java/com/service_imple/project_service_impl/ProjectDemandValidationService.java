package com.service_imple.project_service_impl;

import com.config.ProjectDemandRules;
import com.entity.project_entities.Project;
import com.entity_enums.project_enums.ProjectStage;
import com.entity_enums.project_enums.ProjectStatus;
import com.global_exception_handler.ProjectValidationException;
import com.repo.project_repo.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProjectDemandValidationService {
    private final ProjectRepository projectRepository;

    /* ============================
       ENTRY POINT (USE EVERYWHERE)
       ============================ */
    public Project validateProjectForStaffing(Long pmsProjectId) {

        Project project = projectRepository.findById(pmsProjectId)
                .orElseThrow(() -> new ProjectValidationException(
                        HttpStatus.NOT_FOUND,
                        "PROJECT_NOT_FOUND",
                        "Project does not exist in RMS"
                ));

        validateProjectStatus(project);     // STORY 7
        validateLifecycleStage(project);    // STORY 8

        return project;
    }

    /* ===============================
       STORY 7 — STATUS VALIDATION
       =============================== */
    private void validateProjectStatus(Project project) {

        ProjectStatus status = project.getProjectStatus();

        if (!ProjectDemandRules.ALLOWED_PROJECT_STATUSES.contains(status)) {
            throw new ProjectValidationException(
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
            throw new ProjectValidationException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "INVALID_PROJECT_LIFECYCLE",
                    "Staffing not allowed in lifecycle stage " + stage
            );
        }
    }
}
