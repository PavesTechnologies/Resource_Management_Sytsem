package com.service_interface.project_service_interface;

import com.dto.ApiResponse;
import com.dto.project_dto.*;
import com.entity.project_entities.Project;
import com.entity_enums.centralised_enums.PriorityLevel;
import com.entity_enums.centralised_enums.RiskLevel;
import com.entity_enums.project_enums.ProjectStatus;
import com.entity_enums.project_enums.StaffingReadinessStatus;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

public interface ProjectGovernanceService {

    // 🔹 STORY 9 — Task 2
    ApiResponse<List<ProjectOverlapDTO>> getProjectOverlaps(Long projectId);

    // 🔹 STORY 9 — Task 3
    ApiResponse<DateValidationResponse> validateDemandDates(Long projectId,
                                                            DemandDateValidationRequest request);

    // 🔹 STORY 10 — Task 1
    ApiResponse<List<ProjectListDTO>> getEligibleProjects();

    // 🔹 STORY 10 — Task 3
    ApiResponse<List<ProjectListDTO>> getAllProjectsWithVisibility();

    public ResponseEntity<ApiResponse<Page<ProjectsListDTO>>> getProjectsByManagerId(
            Long managerId,
            int page,
            int size,
            String search,
            StaffingReadinessStatus readinessStatus,
            ProjectStatus projectStatus,
            PriorityLevel priorityLevel,
            RiskLevel riskLevel
    );

    ResponseEntity<?> getProjectById(Long id);
    ResponseEntity<?> getProjectByClient(UUID id);

//    ApiResponse<ProjectGovernanceStatusDTO> validateProjectGovernance(Long id);

}
