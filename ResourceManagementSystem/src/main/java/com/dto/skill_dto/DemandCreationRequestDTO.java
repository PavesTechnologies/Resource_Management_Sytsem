package com.dto.skill_dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DemandCreationRequestDTO {

    @NotNull
    private Long demandId;

    @NotNull
    private Long roleTemplateId;
}

