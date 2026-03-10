package com.dto.allocation_dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class AllocationOverrideReportDTO {

    private UUID allocationId;
    private Long resourceId;
    private Integer allocationPercentage;
    private String overrideJustification;
    private String overrideBy;
    private LocalDateTime overrideAt;
}
