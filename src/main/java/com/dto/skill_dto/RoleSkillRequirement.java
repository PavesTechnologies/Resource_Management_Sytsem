package com.dto.skill_dto;

import lombok.Data;
import java.util.List;

@Data
public class RoleSkillRequirement {
    private String skill;
    private String proficiency;
    private boolean mandatoryFlag;
    private List<RoleRequirementDetail> subSkills;
}
