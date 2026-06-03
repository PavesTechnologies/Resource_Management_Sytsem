package com.dto.skill_dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubSkillCreateDTO {
    
    @NotNull(message = "Skill ID is required")
    private UUID skillId;
    
    @NotEmpty(message = "At least one sub-skill is required")
    private List<SubSkillItemDTO> subSkills;
}
