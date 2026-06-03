package com.dto.demand_dto;

import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class DemandWithSlaResponseDTO {
    // Demand details
    private UUID demandId;
    private String demandName;
    private String demandPriority;
    private Integer priorityScore;
    private String projectPriority;
    private String projectName;
    private Long projectId;

    // Delivery Role details
    private UUID deliveryRoleId;
    private String deliveryRoleName;
    private String deliveryRoleSkill;
    private String deliveryRoleSubSkill;
    private String deliveryRoleProficiency;
    private Boolean deliveryRoleMandatory;

    // SLA details
    private UUID demandSlaId;
    private String slaType;
    private Integer slaDurationDays;
    private Integer warningThresholdDays;
    private LocalDate createdAt;
    private LocalDate dueAt;
    private boolean breached;
    private long remainingDays;
    private long overdueDays;
    private String priorityLevel;
}
