package com.dto.demand_dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryManagerDemandDTO {
    
    // Client Information
    private UUID clientId;
    private String clientName;
    
    // Project Information
    private Long projectId;
    private String projectName;
    
    // Demand Information
    private UUID demandId;
    private String demandName;
    private String demandPriority;
    private Integer priorityScore;
    private String demandStatus;
    private String demandType;
    private String deliveryModel;
    
    // SLA Information
    private UUID demandSlaId;
    private String slaType;
    private Integer slaDurationDays;
    private Integer warningThresholdDays;
    private LocalDate slaCreatedAt;
    private LocalDate slaDueAt;
    private Boolean slaBreached;
    private Integer remainingDays;
    private Integer overdueDays;
}
