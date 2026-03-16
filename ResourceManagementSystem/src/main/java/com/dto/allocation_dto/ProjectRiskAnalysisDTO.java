package com.dto.allocation_dto;

import com.entity_enums.allocation_enums.RoleOffReason;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectRiskAnalysisDTO {
    private String projectName;
    private String projectId;
    private RoleOffReason reason;
    private Long count;
    private Double riskScore;
    private String riskLevel;
}
