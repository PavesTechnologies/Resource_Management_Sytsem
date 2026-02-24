package com.dto.allocation_dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for skill gap analysis - integrated with allocation module
 * Contains comprehensive matching results with scores, risk assessment, and detailed comparisons
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillGapAnalysisResponseDTO {
    
    private UUID demandId;
    private Long resourceId;
    private Double matchPercentage;
    private Boolean allocationAllowed;
    private String riskLevel; // LOW, MEDIUM, HIGH
    private List<SkillComparisonDTO> skillComparisons;
    private List<CertificateComparisonDTO> certificateComparisons;
    private List<RecencyWarningDTO> recencyWarnings;
    
    /**
     * DTO for individual skill comparison results
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkillComparisonDTO {
        private String skillName;
        private String subSkillName; // null if not applicable
        private String requiredProficiency;
        private String resourceProficiency;
        private Boolean mandatory;
        private String status; // MATCH, PARTIAL, GAP
        private Double score; // 1.0, 0.5, 0.0
    }
    
    /**
     * DTO for certificate comparison results
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CertificateComparisonDTO {
        private String certificateName;
        private Boolean mandatory;
        private String status; // MATCH, GAP
        private Double score; // 1.0, 0.0
    }
    
    /**
     * DTO for recency warnings
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecencyWarningDTO {
        private String skillName;
        private String subSkillName; // null if not applicable
        private LocalDate lastUsedDate;
        private String riskLevel; // LOW, MEDIUM, HIGH
        private Long yearsUnused;
    }
}
