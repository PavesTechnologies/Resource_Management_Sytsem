package com.dto.allocation_dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for skill gap analysis - integrated with allocation module
 * Contains demand and resource identifiers for matching analysis
 * Validated for production safety
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillGapAnalysisRequestDTO {
    
    @NotNull(message = "Demand ID is required")
    private UUID demandId;
    
    @NotNull(message = "Resource ID is required")
    private Long resourceId;
}
