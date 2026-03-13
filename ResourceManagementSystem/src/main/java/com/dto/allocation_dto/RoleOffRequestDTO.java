package com.dto.allocation_dto;

import com.entity_enums.allocation_enums.RoleOffReason;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class RoleOffRequestDTO {
    private Long projectId;
    private Long resourceId;
    private UUID roleId;
    private LocalDate roleOffDate;
    private Boolean autoReplacementRequired;
    private String skipReason;
    private RoleOffReason roleOffReason;
}
