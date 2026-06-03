package com.dto.allocation_dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AllocationFailure {
    private String resourceId;
    private String resourceName;
    private String reason;
}