package com.dto.demand_dto;

import com.entity_enums.centralised_enums.PriorityLevel;
import com.entity_enums.demand_enums.DemandType;
import com.entity_enums.demand_enums.DemandStatus;
import com.entity_enums.demand_enums.DemandCommitment;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class DemandResponseDTO {
    private UUID demandId;

    private Long projectId;
    private Long roleId;
    private String demandName;

    private DemandType demandType;
    private DemandStatus demandStatus;

    private Long outgoingResourceId;

    private Boolean requiresAdditionalApproval;

    private PriorityLevel demandPriority;

    private LocalDateTime demandStartDate;
    private LocalDateTime demandEndDate;

    private Integer allocationPercentage;

    private String deliveryModel;

    private String demandJustification;
    private DemandCommitment demandCommitment;

    private Integer minExp;
    private Integer resourcesRequired;

    private Integer softDemandExpiry;
}
