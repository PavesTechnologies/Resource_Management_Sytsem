package com.dto.allocation_dto;

import com.entity.allocation_entities.ResourceAllocation;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Data transfer object for allocation validation results
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AllocationValidationResult {
    private List<ResourceAllocation> validAllocations;
    private List<AllocationFailure> failures;
}
