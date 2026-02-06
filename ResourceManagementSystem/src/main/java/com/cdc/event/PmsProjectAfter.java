package com.cdc.event;

import com.entity_enums.project_enums.ProjectDataStatus;
import com.entity_enums.project_enums.ProjectStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PmsProjectAfter {

    private Long pmsProjectId;
    private String name;
    private ProjectStatus projectStatus;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private ProjectDataStatus dataStatus;
}

