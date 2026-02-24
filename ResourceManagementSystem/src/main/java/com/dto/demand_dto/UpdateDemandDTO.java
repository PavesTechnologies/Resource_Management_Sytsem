package com.dto.demand_dto;

import com.entity_enums.centralised_enums.PriorityLevel;
import com.entity_enums.demand_enums.DemandType;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class UpdateDemandDTO {
    private UUID demandId;

    private DemandType demandType;

    private Long outgoingResourceId;

    private String demandJustification;

    private LocalDate demandStartDate;
    private LocalDate demandEndDate;

    private Integer allocationPercentage;

    private String locationRequirement;
    private String deliveryModel;

    private PriorityLevel demandPriority;
}
