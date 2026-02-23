package com.dto.skill_dto;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class RoleExpectationWithMandatoryResponse {

    private UUID dev_role_id;
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
