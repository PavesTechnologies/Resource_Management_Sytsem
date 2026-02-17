package com.dto.skill_dto;

import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

@Data
public class DeliveryRoleExpectationRequest {

    private String roleName;

    @NotNull(message = "At least one expectation is required")
    private List<ExpectationDetail> expectations;

    @Data
    public static class ExpectationDetail {
        
        @NotNull(message = "Skill ID is required")
        private UUID skillId;

        private UUID subSkillId;

        @NotNull(message = "Proficiency ID is required")
        private UUID proficiencyId;

        @NotNull(message = "Each skill must be marked as Mandatory or Optional")
        private Boolean mandatoryFlag;
    }
}
