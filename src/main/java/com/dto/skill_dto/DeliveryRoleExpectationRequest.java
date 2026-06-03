package com.dto.skill_dto;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

@Data
public class DeliveryRoleExpectationRequest {

    private String roleName;

    @NotNull(message = "At least one skill is required")
    private List<SkillExpectation> skills;

    @Data
    public static class SkillExpectation {
        
        @NotNull(message = "Skill ID is required")
        @JsonProperty("skillId")
        private UUID skillId;

        @NotNull(message = "Proficiency ID is required")
        @JsonProperty("proficiencyId")
        private UUID proficiencyId;

        @NotNull(message = "Each skill must be marked as Mandatory or Optional")
        @JsonProperty("mandatoryFlag")
        private Boolean mandatoryFlag;

        @JsonProperty("subSkills")
        private List<SubSkillExpectation> subSkills;
    }

    @Data
    public static class SubSkillExpectation {
        
        @NotNull(message = "SubSkill ID is required")
        @JsonProperty("subSkillId")
        private UUID subSkillId;

        @NotNull(message = "Proficiency ID is required")
        @JsonProperty("proficiencyId")
        private UUID proficiencyId;

        @NotNull(message = "Each subskill must be marked as Mandatory or Optional")
        @JsonProperty("mandatoryFlag")
        private Boolean mandatoryFlag;
    }
}
