package com.dto.skill_dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubSkillCreateDTO {
    
    @NotNull(message = "Skill ID is required")
    private UUID skillId;
    
    @NotBlank(message = "Sub-skill name is required")
    private String name;
    
    private String description;
}
