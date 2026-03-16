package com.dto.allocation_dto;

import com.entity_enums.allocation_enums.AllocationModificationStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class AllocationModificationResponseDTO {
    
    private UUID modificationId;
    private UUID allocationId;
    private String resourceName;
    private Integer currentAllocationPercentage;
    private Integer requestedAllocationPercentage;
    private String effectiveDate;
    private AllocationModificationStatus status;
    private String requestedBy;
    private String approvedBy;
    private LocalDateTime requestedAt;
    private LocalDateTime approvedAt;
    private String reason;
    private String rejectReason;
    private String rejectedBy;
    private Boolean overrideFlag;
    private String overrideJustification;
    private String overrideBy;
    private LocalDateTime overrideAt;
}
