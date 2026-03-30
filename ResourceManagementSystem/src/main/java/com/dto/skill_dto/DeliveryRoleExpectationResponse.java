package com.dto.skill_dto;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class DeliveryRoleExpectationResponse {
    private UUID dev_role_id;
    private String role;
    private List<RoleSkillRequirement> skills;
}
