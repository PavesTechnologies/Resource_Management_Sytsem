package com.dto.skill_dto;

import com.entity_enums.skill_enums.ProficiencyLevel;
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

        @NotNull(message = "Proficiency level is required")
        private ProficiencyLevel proficiencyLevel;
    }
}
