package com.dto.demand_dto;

import com.entity_enums.centralised_enums.DeliveryModel;
import com.entity_enums.centralised_enums.PriorityLevel;
import com.entity_enums.demand_enums.DemandStatus;
import com.entity_enums.demand_enums.DemandType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class CreateDemandDTO {
    @NotNull(message = "Demand name is required")
    private String demandName;
    @NotNull(message ="Project ID is required")
    private Long projectId;
    @NotNull(message = "Role ID is required")
    private UUID roleId;

    @NotNull(message = "Demand type is required")
    private DemandType demandType;

    // Required only for REPLACEMENT (validated in service)
    private Long outgoingResourceId;

    @NotNull(message = "Start date is required")
    private LocalDate demandStartDate;

    private LocalDate demandEndDate;

    @Min(value = 1, message = "Allocation must be at least 1%")
    @Max(value = 100, message = "Allocation cannot exceed 100%")
    private Integer allocationPercentage;

    private String locationRequirement;
    @NotNull(message = "Delivery model is required")
    private DeliveryModel deliveryModel;

    @NotNull(message = "Demand status is required")
    private DemandStatus demandStatus;

    private PriorityLevel demandPriority;

    @NotNull(message = "Minimum experience is required")
    private Integer minExp;
    @NotNull(message = "Maximum experience is required")
    @Min(value = 1, message = "Resources required must be at least 1")
    private Integer resourceRequired;

    private String demandJustification;
}
