package com.dto.project_dto;

import com.entity_enums.project_enums.StaffingReadinessStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReadinessStatusDTO {
    private StaffingReadinessStatus status;
    private String reason;
}
