package com.dto.allocation_dto;

import com.entity_enums.roleoff_enums.RoleOffReason;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleOffReasonStatsDTO {
    private RoleOffReason reason;
    private Long count;
    private Double percentage;
    private String description;
}
