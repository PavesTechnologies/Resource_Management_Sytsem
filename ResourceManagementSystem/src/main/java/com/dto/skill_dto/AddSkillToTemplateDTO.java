package com.dto.skill_dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddSkillToTemplateDTO {

    @NotNull
    private Long subSkillId;

    @NotNull
    private Long proficiencyId;
}

