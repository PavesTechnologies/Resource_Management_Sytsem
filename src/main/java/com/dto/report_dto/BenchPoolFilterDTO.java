package com.dto.report_dto;

import com.entity_enums.centralised_enums.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class BenchPoolFilterDTO {
    
    private String skill;
    private String role;
    private String region;
    private Long minBenchDays;
    private Long maxBenchDays;
    private BigDecimal maxCost;
    private RiskLevel riskLevel;
    
    @Builder.Default
    private Integer page = 0;
    
    @Builder.Default
    private Integer size = 50;
}
