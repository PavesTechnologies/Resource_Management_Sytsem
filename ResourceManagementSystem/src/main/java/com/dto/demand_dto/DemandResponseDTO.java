package com.dto.demand_dto;

import com.entity_enums.centralised_enums.PriorityLevel;
import com.entity_enums.demand_enums.DemandType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class DemandResponseDTO {
    private UUID demandId;

    private Long projectId;
    private Long roleId;

    private DemandType demandType;

    private Long outgoingResourceId;

    private Boolean requiresAdditionalApproval;

    private PriorityLevel demandPriority;

    private LocalDate demandStartDate;
    private LocalDate demandEndDate;

    private Integer allocationPercentage;

    private String locationRequirement;
    private String deliveryModel;

    private String demandJustification;
}
