package com.dto.allocation_dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AllocationFailure {
    private Long resourceId;
//    private String resourceName;
    private String reason;
}