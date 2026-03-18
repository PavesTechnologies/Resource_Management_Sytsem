package com.dto.allocation_dto;

import com.entity_enums.allocation_enums.RoleOffType;
import com.entity_enums.allocation_enums.RoleOffReason;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class RoleOffRequestDTO {
    private Long projectId;
    private Long resourceId;
    private UUID allocationId; // Optional: Specify exact allocation when multiple exist
    private UUID replacementRoleId; // Optional: Used when allocation doesn't have demand with role
    private RoleOffType roleOffType;
    private LocalDate roleOffDate;
    private Boolean autoReplacementRequired;
    private String skipReason;
    private RoleOffReason roleOffReason;
    private String emergencyReason;
    private Boolean confirmed; // Added for delivery impact confirmation
}
