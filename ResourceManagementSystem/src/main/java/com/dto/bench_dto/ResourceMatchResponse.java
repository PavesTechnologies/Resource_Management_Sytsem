package com.dto.bench_dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceMatchResponse {
    
    private Long resourceId;
    private String resourceName;
    private Integer resourceExperience;
    private String availability;
    private List<DemandMatch> demands;
}
