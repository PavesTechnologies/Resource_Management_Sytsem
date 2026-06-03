package com.dto.allocation_dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class CreateAllocationModificationDTO {
    
    @NotNull(message = "Allocation ID is required")
    private UUID allocationId;
    
    @NotNull(message = "Requested allocation percentage is required")
    @Min(value = 0, message = "Requested allocation must be at least 0%")
    @Max(value = 150, message = "Requested allocation cannot exceed 150%")
    private Integer requestedAllocationPercentage;
    
    @NotNull(message = "Effective date is required")
    private LocalDate effectiveDate;
    
    /**
     * Override end date (optional for ≤100%, mandatory for >100%)
     * Required only when total allocation (existing + requested) > 100%
     * Override will end on this date
     */
    private LocalDate overrideEndDate;
    
    @NotNull(message = "Reason for modification is required")
    private String reason;
    
    /**
     * User making the request (optional, can be filled from context)
     */
    private String requestedBy;
}
