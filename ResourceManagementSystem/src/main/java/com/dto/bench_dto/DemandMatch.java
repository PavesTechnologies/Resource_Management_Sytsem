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
public class DemandMatch {
    
    private UUID demandId;
    private String demandName;
    private List<String> matchedSkills;
    private double matchScore;
}
