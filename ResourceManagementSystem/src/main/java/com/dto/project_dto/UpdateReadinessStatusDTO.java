package com.dto.project_dto;

import com.entity_enums.project_enums.StaffingReadinessStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("pmsProjectId")
    private Long pmsProjectId;

    @NotNull(message = "status is required")
    @JsonProperty("status")
    private StaffingReadinessStatus status;

    @NotBlank(message = "reason is required")
    @JsonProperty("reason")
    private String reason;
}
