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
public class DemandSlaResponseDTO {
    private UUID demandId;
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
