package com.dto.allocation_dto;

import com.entity_enums.allocation_enums.RoleOffType;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class RoleOffRequestDTO {
    private Long projectId;
    private Long resourceId;
    private UUID roleId;
    private RoleOffType roleOffType;
    private LocalDate roleOffDate;
    private Boolean autoReplacementRequired;
    private String skipReason;
    private String emergencyReason;
}
