package com.dto.skill_dto;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class DeliveryRoleExpectationResponse {
    private UUID dev_role_id;
    private String role;
    private List<SkillRequirement> skills;

    @Data
    public static class SkillRequirement {
        
        private String skill;
        private List<RequirementDetail> requirements;
    }

    @Data
    public static class RequirementDetail {
        
        private String subSkill;
        private String proficiency;
        private Boolean mandatoryFlag;
    }
}
