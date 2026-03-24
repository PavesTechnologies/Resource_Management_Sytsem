package com.dto.allocation_dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class AllocationResponseDTO {
    private UUID allocationId;
    private String fullName;
    private String email;
    private String demandName;
    private LocalDate allocationStartDate;
    private LocalDate allocationEndDate;
    private Integer allocationPercentage;
    private String allocationStatus;
    private String createdBy;
    private Integer remainingAllocationPercentage;
}
