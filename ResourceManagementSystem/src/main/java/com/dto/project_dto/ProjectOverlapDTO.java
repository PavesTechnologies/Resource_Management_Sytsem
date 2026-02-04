package com.dto.project_dto;

import java.time.LocalDateTime;

public record ProjectOverlapDTO(
        Long projectId,
        String projectName,
        LocalDateTime startDate,
        LocalDateTime endDate
) {}
