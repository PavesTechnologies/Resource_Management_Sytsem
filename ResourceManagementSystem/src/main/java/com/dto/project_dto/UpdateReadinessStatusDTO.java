package com.dto.project_dto;

import com.entity_enums.project_enums.StaffingReadinessStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateReadinessStatusDTO {
    @NotNull(message = "pmsProjectId is required")
    private Long pmsProjectId;

    @NotNull(message = "status is required")
    private StaffingReadinessStatus status;

    @NotBlank(message = "reason is required")
    private String reason;
}
