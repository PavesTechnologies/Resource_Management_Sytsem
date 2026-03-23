package com.dto.roleoff_dto;

import com.entity.allocation_entities.ResourceAllocation;
import com.entity.project_entities.Project;
import com.entity.resource_entities.Resource;
import com.entity_enums.allocation_enums.RoleOffStatus;
import com.entity_enums.allocation_enums.RoleOffType;
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
}
