package com.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClientProjectStatisticsDTO {
    private Long totalProjects;
    private Long activeProjects;
    private BigDecimal totalSpend;
}
