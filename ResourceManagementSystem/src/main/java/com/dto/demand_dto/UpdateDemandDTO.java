package com.dto.demand_dto;

import com.entity_enums.centralised_enums.DeliveryModel;
import com.entity_enums.centralised_enums.PriorityLevel;
import com.entity_enums.client_enums.SLAType;
import com.entity_enums.demand_enums.DemandType;
import com.entity_enums.demand_enums.DemandStatus;
import com.entity_enums.demand_enums.DemandCommitment;
import lombok.Data;

import java.time.LocalDateTime;
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

    private DeliveryModel deliveryModel;

    private PriorityLevel demandPriority;

    private DemandStatus demandStatus;

    private DemandCommitment demandCommitment;

    private Boolean requiresAdditionalApproval;

    private Integer resourcesRequired;

    private Long modifiedBy;
}
