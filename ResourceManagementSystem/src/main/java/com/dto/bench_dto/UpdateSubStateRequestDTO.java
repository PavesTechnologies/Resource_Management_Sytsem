package com.dto.bench_dto;

import com.entity_enums.bench.SubState;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateSubStateRequestDTO {
    @NotNull(message = "Resource ID cannot be null")
    private Long resourceId;

    @NotNull(message = "New sub-state cannot be null")
    private SubState newSubState;
    @NotNull(message = "Reason cannot be null")
    private String reason;
}
