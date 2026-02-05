package com.service_interface.project_service_interface;

import com.dto.ApiResponse;
import com.dto.project_dto.DateValidationResponse;
import com.dto.project_dto.DemandDateValidationRequest;
import com.dto.project_dto.ProjectListDTO;
import com.dto.project_dto.ProjectOverlapDTO;
import com.entity.project_entities.Project;

import java.util.List;

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

    ApiResponse<List<Project>> getProjectsByManagerId(Long managerId);
}
