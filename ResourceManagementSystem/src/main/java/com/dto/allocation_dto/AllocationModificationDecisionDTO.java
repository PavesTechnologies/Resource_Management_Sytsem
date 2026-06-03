package com.dto.allocation_dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AllocationModificationDecisionDTO {
    
    @NotNull(message = "Decision is required")
    private String decision; // "APPROVE" or "REJECT"
    
    private String rejectionReason; // Required only when decision is "REJECT"
}
