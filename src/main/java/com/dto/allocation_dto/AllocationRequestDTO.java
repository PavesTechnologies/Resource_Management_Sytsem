package com.dto.allocation_dto;

import com.entity_enums.allocation_enums.AllocationStatus;
import com.entity_enums.allocation_enums.AllocationType;
import com.entity_enums.roleoff_enums.RoleOffReason;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AllocationRequestDTO {


    @NotNull(message = "Resource ID is required")
    private List<String> resourceId;

    private UUID demandId;

    private Long projectId;

    @NotNull(message = "Allocation start date is required")
    @FutureOrPresent(message = "Allocation start date must be today or in the future")
    private LocalDate allocationStartDate;
    @NotNull(message = "Allocation end date is required")
    private LocalDate allocationEndDate;

    @Min(value = 1, message = "Allocation percentage must be at least 1")
    @Max(value = 130, message = "Allocation percentage cannot exceed 130")
    private Integer allocationPercentage;

    private String overrideJustification;

    @NotNull(message = "Allocation status is required")
    private AllocationStatus allocationStatus;

    /**
     * Allocation Type - ACTIVE (starts immediately) or PLANNED (future start date)
     * If not provided, defaults to ACTIVE
     */
    private AllocationType allocationType;

    /**
     * Planned start date for PLANNED allocation type
     * Required if allocationType is PLANNED, ignored for ACTIVE type
     */
    private LocalDate plannedStartDate;

    private String createdBy;

    private LocalDate roleOffDate;

    private RoleOffReason roleOffReason;
    
    private Boolean skipValidation = false;

    private Boolean requestBeyondCapacityApproval;
    
    @AssertTrue(message = "Either demandId or projectId must be provided, but not both")
    public boolean isEitherDemandOrProjectProvided() {
        return (demandId != null && projectId == null) || (projectId != null && demandId == null);
    }
    
    @AssertTrue(message = "Allocation end date must be after or equal to start date")
    public boolean isEndDateValid() {
        return allocationEndDate != null && allocationStartDate != null 
               && !allocationEndDate.isBefore(allocationStartDate);
    }

    @AssertTrue(message = "PLANNED allocation requires a planned start date in the future")
    public boolean isPlannedAllocationValid() {
        if (allocationType == com.entity_enums.allocation_enums.AllocationType.PLANNED) {
            return plannedStartDate != null && plannedStartDate.isAfter(java.time.LocalDate.now());
        }
        return true;
    }

    @AssertTrue(message = "ACTIVE allocation cannot have a planned start date")
    public boolean isActiveAllocationValid() {
        if (allocationType == com.entity_enums.allocation_enums.AllocationType.ACTIVE) {
            return plannedStartDate == null;
        }
        return true;
    }
}
