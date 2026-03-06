package com.dto.demand_dto;

import com.entity_enums.demand_enums.DemandStatus;
import lombok.Data;

import java.util.UUID;

@Data
public class DemandDecisionDTO {
    private UUID demandId;
    private DemandStatus decision;
    private String rejectionReason;

}
