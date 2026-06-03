package com.dto.skill_dto;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

@Data
public class RoleExpectationRequest {

    @NotNull(message = "Role ID is required")
    @JsonProperty("roleId")
    private UUID roleId;

    @NotNull(message = "Skills list is required")
    @Valid
    private List<SkillExpectationDto> skills;

    @Data
    public static class SkillExpectationDto {
        
        @NotNull(message = "Skill ID is required")
        @JsonProperty("skillId")
        private UUID skillId;

        @NotNull(message = "Mandatory flag is required")
        @JsonProperty("mandatoryFlag")
        private Boolean mandatoryFlag;

        @NotNull(message = "Proficiency ID is required")
        @JsonProperty("proficiencyId")
        private UUID proficiencyId;

        @NotNull(message = "SubSkills list cannot be null")
        @Valid
        @JsonProperty("subSkills")
        private List<SubSkillExpectationDto> subSkills;
    }

    @Data
    public static class SubSkillExpectationDto {
        
        @NotNull(message = "SubSkill ID is required")
        @JsonProperty("subSkillId")
        private UUID subSkillId;

        @NotNull(message = "Proficiency ID is required")
        @JsonProperty("proficiencyId")
        private UUID proficiencyId;

        @NotNull(message = "Mandatory flag is required")
        @JsonProperty("mandatoryFlag")
        private Boolean mandatoryFlag;
    }
}
