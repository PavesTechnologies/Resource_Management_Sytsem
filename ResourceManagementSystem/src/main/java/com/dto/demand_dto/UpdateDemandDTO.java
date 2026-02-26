package com.dto.demand_dto;

import com.entity_enums.centralised_enums.DeliveryModel;
import com.entity_enums.centralised_enums.PriorityLevel;
import com.entity_enums.demand_enums.DemandType;
import com.entity_enums.demand_enums.DemandStatus;
import com.entity_enums.demand_enums.DemandCommitment;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class UpdateDemandDTO {
    private UUID demandId;

    private DemandType demandType;

    private Long outgoingResourceId;

    private String demandJustification;

    private LocalDateTime demandStartDate;
    private LocalDateTime demandEndDate;

    private Integer allocationPercentage;

    private DeliveryModel deliveryModel;

    private PriorityLevel demandPriority;

    private DemandStatus demandStatus;

    private DemandCommitment demandCommitment;

    private Integer softDemandExpiry;

    private Boolean requiresAdditionalApproval;
}
