package com.dto.allocation_dto;

import com.entity_enums.allocation_enums.AllocationStatus;
import com.entity_enums.roleoff_enums.RoleOffReason;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AllocationRequestDTO {


    @NotNull(message = "Resource ID is required")
    private List<Long> resourceId;

    private UUID demandId;

    private Long projectId;

    @NotNull(message = "Allocation start date is required")
    @FutureOrPresent(message = "Allocation start date must be today or in the future")
    private LocalDate allocationStartDate;
    @NotNull(message = "Allocation end date is required")
    private LocalDate allocationEndDate;

    @NotNull(message = "Allocation percentage is required")
    @Min(value = 1, message = "Allocation percentage must be at least 1")
    @Max(value = 130, message = "Allocation percentage cannot exceed 130")
    private Integer allocationPercentage;

    private String overrideJustification;

    @NotNull(message = "Allocation status is required")
    private AllocationStatus allocationStatus;

    private String createdBy;

    private LocalDate roleOffDate;

    private RoleOffReason roleOffReason;
    
    private Boolean skipValidation = false;
    
    @AssertTrue(message = "Either demandId or projectId must be provided, but not both")
    public boolean isEitherDemandOrProjectProvided() {
        return (demandId != null && projectId == null) || (projectId != null && demandId == null);
    }
    
    @AssertTrue(message = "Allocation end date must be after or equal to start date")
    public boolean isEndDateValid() {
        return allocationEndDate != null && allocationStartDate != null 
               && !allocationEndDate.isBefore(allocationStartDate);
    }
}
