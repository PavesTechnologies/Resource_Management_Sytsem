package com.dto.skill_dto;

import lombok.Data;

import java.util.List;

@Data
public class DeliveryRoleExpectationResponse {

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
