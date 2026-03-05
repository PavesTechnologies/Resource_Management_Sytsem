package com.dto.allocation_dto;

import com.entity_enums.centralised_enums.ClientTier;
import com.entity_enums.demand_enums.DemandCommitment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AllocationConflictDTO {
    
    private UUID conflictId;
    private Long resourceId;
    private String resourceName;
    private AllocationDetails lowerPriorityAllocation;
    private AllocationDetails higherPriorityAllocation;
    private String conflictType;
    private String conflictSeverity;
    private String recommendation;
    private List<ResolutionOption> resolutionOptions;
    private String resolutionStatus;
    private LocalDateTime detectedAt;
    private LocalDateTime resolvedAt;
    private String resolvedBy;
    private String resolutionNotes;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AllocationDetails {
        private UUID allocationId;
        private String clientName;
        private ClientTier clientTier;
        private DemandCommitment allocationType;
        private LocalDate startDate;
        private LocalDate endDate;
        private Integer allocationPercentage;
        private String projectName;
        private String demandName;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ResolutionOption {
        private String action; // "UPGRADE", "DISPLACE", "KEEP_CURRENT"
        private String description;
        private String impact;
        private boolean recommended;
    }
}
