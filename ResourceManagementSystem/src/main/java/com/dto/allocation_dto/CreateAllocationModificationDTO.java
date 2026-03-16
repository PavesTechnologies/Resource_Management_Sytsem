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
    @Max(value = 100, message = "Requested allocation cannot exceed 100%")
    private Integer requestedAllocationPercentage;
    
    @NotNull(message = "Effective date is required")
    private LocalDate effectiveDate;
    
    @NotNull(message = "Reason for modification is required")
    private String reason;
    
    private String overrideJustification;
}
