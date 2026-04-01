package com.dto.roleoff_dto;

import com.entity_enums.roleoff_enums.RoleOffType;
import com.entity_enums.roleoff_enums.ResourcePerformance;
import lombok.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class RoleOffRequestDTO {
    private Long projectId;
    private List<Long> resourceIds;
    private List<UUID> allocationId;
    private RoleOffType roleOffType;
    private LocalDate effectiveRoleOffDate;
    private String roleOffReason;
    private Boolean confirmed = false; // Added for delivery impact confirmation
    
    // NEW FIELD from RoleOffEvent entity
    private ResourcePerformance resourcePerformance;
}
