package com.dto.allocation_dto;

import com.entity_enums.roleoff_enums.RoleOffType;
import com.entity_enums.roleoff_enums.RoleOffReason;
import com.entity_enums.roleoff_enums.ResourcePerformance;
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
    private LocalDate effectiveRoleOffDate;
    private Boolean autoReplacementRequired;
    private String skipReason;
    private RoleOffReason roleOffReason;
    private String emergencyReason;
    private Boolean confirmed = false; // Added for delivery impact confirmation
    
    // NEW FIELD from RoleOffEvent entity
    private ResourcePerformance resourcePerformance;
}
