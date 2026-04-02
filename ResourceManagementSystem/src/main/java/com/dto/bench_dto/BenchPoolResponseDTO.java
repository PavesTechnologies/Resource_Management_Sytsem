package com.dto.bench_dto;

import com.entity_enums.bench.SubState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BenchPoolResponseDTO {
    private Long employeeId;
    private String resourceName;
    private String designation;
    private List<Map<String, String>> skillGroups;
    private SubState subState;
    private Integer allocation;
    private Integer aging;
    private Double costPerDay;
    private LocalDate lastAllocationDate;
    private String location;
    private Long experience;
}
