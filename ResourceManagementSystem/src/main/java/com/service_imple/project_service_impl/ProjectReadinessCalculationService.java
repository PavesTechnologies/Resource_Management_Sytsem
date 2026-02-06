package com.service_imple.project_service_impl;

import com.dto.project_dto.ReadinessStatusDTO;
import com.entity.project_entities.Project;
import com.entity_enums.project_enums.ProjectDataStatus;
import com.entity_enums.project_enums.ProjectStatus;
import com.entity_enums.project_enums.StaffingReadinessStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ProjectReadinessCalculationService {

    public ReadinessStatusDTO calculate(Project project) {

        if (project.getDataStatus() != ProjectDataStatus.COMPLETE) {
            return notReady("Project data incomplete from PMS");
        }

        if (project.getProjectStatus() != ProjectStatus.APPROVED &&
                project.getProjectStatus() != ProjectStatus.ACTIVE) {
            return notReady("Project not approved");
        }

        if (project.getStartDate() == null) {
            return notReady("Project dates not finalized");
        }

        if (project.getStartDate().isAfter(LocalDateTime.now().plusDays(30))) {
            return upcoming("Project start date is in future");
        }

        return ready();
    }

    private ReadinessStatusDTO notReady(String reason) {
        return new ReadinessStatusDTO(
                StaffingReadinessStatus.NOT_READY,
                reason
        );
    }

    private ReadinessStatusDTO upcoming(String reason) {
        return new ReadinessStatusDTO(
                StaffingReadinessStatus.UPCOMING,
                reason
        );
    }

    private ReadinessStatusDTO ready() {
        return new ReadinessStatusDTO(
                StaffingReadinessStatus.READY,
                "Project ready for staffing"
        );
    }
}
