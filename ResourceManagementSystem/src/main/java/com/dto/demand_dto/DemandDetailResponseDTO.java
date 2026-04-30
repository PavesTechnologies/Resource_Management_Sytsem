package com.dto.demand_dto;

import com.entity.skill_entities.DeliveryRoleExpectation;
import com.entity_enums.demand_enums.DemandCommitment;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class DemandDetailResponseDTO {
    // Client details
    private UUID clientId;
    private String clientName;
    
    // Project details
    private Long projectId;
    private String projectName;
    
    // Demand details
    private UUID demandId;
    private String demandName;
    private String demandPriority;
    private String deliveryRole;
    private Integer priorityScore;
    private String demandStatus;
    private Double minExp;
    private Integer resourceRequired;
    private Integer allocation;
    private String demandJustification;
    private String demandType;
    private String deliveryModel;
    private DemandCommitment demandCommitment;
    private LocalDate demandStartDate;
    private LocalDate demandEndDate;
    private String rejectionReason;
    private String rmRejectionReason;
    private String dmRejectionReason;
    
    // SLA details
    private UUID demandSlaId;
    private String slaType;
    private Integer slaDurationDays;
    private Integer warningThresholdDays;
    private LocalDate slaCreatedAt;
    private LocalDate slaDueAt;
    private boolean slaBreached;
    private long remainingDays;
    private long overdueDays;
    private LocalDate fulfilledDate;
}
