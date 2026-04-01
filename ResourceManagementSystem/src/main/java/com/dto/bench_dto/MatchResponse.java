package com.dto.bench_dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchResponse {
    
    private Long resourceId;
    private String resourceName;
    private Integer resourceExperience;
    private double matchScore;
    private List<String> matchedSkills;
    private String availability;
    private UUID demandId;
    private String demandName;
}
