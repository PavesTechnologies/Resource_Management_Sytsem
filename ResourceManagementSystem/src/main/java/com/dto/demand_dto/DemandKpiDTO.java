package com.dto.demand_dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemandKpiDTO {
    private Long active;
    private Long soft;
    private Long pending;
    private Long approved;
    private Long slaAtRisk;
    private Long slaBreached;
}
