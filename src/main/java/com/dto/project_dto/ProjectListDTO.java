package com.dto.project_dto;

import com.entity_enums.project_enums.ProjectStatus;
import com.entity_enums.project_enums.ProjectStage;

public record ProjectListDTO(
        Long projectId,
        String name,
        ProjectStatus status,
        ProjectStage stage,
        boolean eligibleForDemand,
        String visibilityReason
) {}
