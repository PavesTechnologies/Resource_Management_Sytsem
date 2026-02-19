package com.dto.skill_dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateRoleTemplateRequestDTO {

    @NotBlank
    private String roleName;

    @NotNull
    private Integer minExperienceYears;

    @NotNull
    private Integer maxExperienceYears;
}

