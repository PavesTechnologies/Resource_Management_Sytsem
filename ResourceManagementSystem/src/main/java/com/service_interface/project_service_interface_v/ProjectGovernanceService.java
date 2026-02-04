package com.service_interface.project_service_interface_v;

import com.dto.client_dto.ApiResponse;
import com.dto.project_dto_v.DateValidationResponse;
import com.dto.project_dto_v.DemandDateValidationRequest;
import com.dto.project_dto_v.ProjectListDTO;
import com.dto.project_dto_v.ProjectOverlapDTO;

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
}
