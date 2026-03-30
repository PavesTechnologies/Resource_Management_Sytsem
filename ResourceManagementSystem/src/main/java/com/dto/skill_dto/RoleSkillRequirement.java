package com.dto.skill_dto;

import lombok.Data;
import java.util.List;

@Data
public class RoleSkillRequirement {
    private String skill;
    private String subSkill;
    private String proficiency;
    private List<RoleRequirementDetail> requirements;
}
