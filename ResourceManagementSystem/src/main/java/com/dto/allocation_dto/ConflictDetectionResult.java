package com.dto.allocation_dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConflictDetectionResult {
    
    private boolean hasConflicts;
    private String conflictType; // "PRIORITY_CONFLICT", "CAPACITY_CONFLICT", "DETECTION_ERROR"
    private String severity; // "HIGH", "MEDIUM", "LOW"
    private String message;
    private List<PriorityConflictDetail> conflicts;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PriorityConflictDetail {
        private UUID conflictId;
        private Long resourceId;
        private String resourceName;
        private UUID existingAllocationId;
        private AllocationRequestSummary newAllocationRequest;
        private String conflictType;
        private String severity;
        private String message;
        private String existingClientName;
        private String existingClientTier;
        private String existingAllocationType;
        private String newClientName;
        private String newClientTier;
        private String newAllocationType;
        private List<String> recommendedActions;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AllocationRequestSummary {
        private UUID demandId;
        private Long projectId;
        private Long resourceId;
        private String allocationStatus; // "PLANNED", "ACTIVE"
        private LocalDate allocationStartDate;
        private LocalDate allocationEndDate;
        private Integer allocationPercentage;
    }
}
