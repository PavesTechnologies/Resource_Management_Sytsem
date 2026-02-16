package com.dto.skill_dto;

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
public class SkillWithSubSkillDTO {
    
    @NotNull(message = "Skill ID is required")
    private UUID skillId;
    
    @NotNull(message = "Proficiency ID is required")
    private UUID proficiencyId;
    
    private List<SubSkillDTO> subSkills;
}
