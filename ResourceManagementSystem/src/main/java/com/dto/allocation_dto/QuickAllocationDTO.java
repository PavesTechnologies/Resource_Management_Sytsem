package com.dto.allocation_dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuickAllocationDTO {

    @NotNull(message = "Resource ID is required")
    private Long resourceId;

    @NotNull(message = "Demand ID is required")
    private UUID demandId;

    @Min(value = 1, message = "Allocation percentage must be at least 1%")
    @Max(value = 100, message = "Allocation percentage cannot exceed 100%")
    private Integer allocationPercentage = 100;
}
