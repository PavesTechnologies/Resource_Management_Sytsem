package com.dto.project_dto;

import com.entity_enums.centralised_enums.PriorityLevel;
import com.entity_enums.centralised_enums.RiskLevel;
import com.entity_enums.project_enums.ProjectStatus;
import com.entity_enums.project_enums.StaffingReadinessStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectsListDTO {
    private long projectId;
    private String projectName;
    private ProjectStatus projectStatus;
    private RiskLevel riskLevel;
    private PriorityLevel projectPriorityLevel;
    private String clientName;
    private PriorityLevel clientPriorityLevel;
    private StaffingReadinessStatus readinessStatus;
    private String reason;
    private BigDecimal projectBudget;
    private boolean hasOverlap;
}
