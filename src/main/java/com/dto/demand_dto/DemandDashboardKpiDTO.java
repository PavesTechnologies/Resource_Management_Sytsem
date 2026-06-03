package com.dto.demand_dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemandDashboardKpiDTO {
    private Long total;
    private Long active;
    private Long fulfilled;
    private Long soft;
    private Long pending;
    private Long approved;
}
