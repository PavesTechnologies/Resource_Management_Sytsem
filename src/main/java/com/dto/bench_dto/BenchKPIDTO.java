package com.dto.bench_dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BenchKPIDTO {
    private Long totalBenchResources;
    private Long totalReadyNowResources;
    private Long totalPoolResources;
    private Long totalRiskWatch;
}
