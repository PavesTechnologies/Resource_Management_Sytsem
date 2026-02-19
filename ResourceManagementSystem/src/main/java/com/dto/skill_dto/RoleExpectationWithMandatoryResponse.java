package com.dto.skill_dto;

import lombok.Data;

import java.util.List;

@Data
public class RoleExpectationWithMandatoryResponse {

    private String roleName;
    private List<SkillRequirement> mandatorySkills;
    private List<SkillRequirement> optionalSkills;

    @Data
    public static class SkillRequirement {
        
        private String skill;
        private String subSkill;
        private String proficiency;
    }
}
