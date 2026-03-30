package com.dto.skill_dto;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class RoleExpectationWithMandatoryResponse {

    private UUID dev_role_id;
    private String roleName;
    private List<RoleSkillRequirement> mandatorySkills;
    private List<RoleSkillRequirement> optionalSkills;
}
