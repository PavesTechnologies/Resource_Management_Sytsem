package com.dto.project_dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectKpiDTO {
    
    private Long totalProjects;
    private Long activeProjects;
    private Long highRiskProjects;
    private Double avgResourceUtil;
}
