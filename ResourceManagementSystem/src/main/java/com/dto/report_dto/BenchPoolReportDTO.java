package com.dto.report_dto;

import com.entity_enums.centralised_enums.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BenchPoolReportDTO {
    
    private Long resourceId;
    private String name;
    private String status;
    private List<String> skills;
    private String role;
    private String region;
    private String lastProject;
    private String client;
    private Long benchDays;
    private BigDecimal cost;
    private RiskLevel riskLevel;
    private String riskType;
    private String recommendedAction;
}
