package com.dto.roleoff_dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectRoleOffKPIDTO {
    private Long activeAllocations;
    private Long pendingRoleOffs;
    private Long totalRoleOff;
}
