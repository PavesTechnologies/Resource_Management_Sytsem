package com.dto.roleoff_dto;

import com.entity_enums.allocation_enums.RoleOffReason;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class BulkRoleOffRequestDTO {
    private Long projectId;
    private List<UUID> allocationIds; // List of allocation IDs to role off
    private LocalDate effectiveRoleOffDate;
    private RoleOffReason roleOffReason;
    private Boolean confirmed = false; // For delivery impact confirmation
    
    // Validation method
    public boolean isValidForBulkPlannedRoleOff() {
        return projectId != null && 
               allocationIds != null && !allocationIds.isEmpty() &&
               effectiveRoleOffDate != null && 
               effectiveRoleOffDate.isEqual(LocalDate.now()) || effectiveRoleOffDate.isAfter(LocalDate.now()) &&
               roleOffReason != null;
    }
}
