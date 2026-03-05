package com.dto.allocation_dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class AllocationResponseDTO {
    private UUID allocationId;

    private Long resourceId;
    private String resourceName;

    private UUID demandId;
    private Long projectId;

    private LocalDate allocationStartDate;
    private LocalDate allocationEndDate;

    private Integer allocationPercentage;

    private String allocationStatus;
}
