package com.dto.project_dto_v;

import com.entity_enums.project_enums_v.ProjectStatus;
import com.entity_enums.project_enums_v.ProjectStage;

public record ProjectListDTO(
        Long projectId,
        String name,
        ProjectStatus status,
        ProjectStage stage,
        boolean eligibleForDemand,
        String visibilityReason
) {}
