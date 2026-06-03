package com.dto.skill_dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeSkillRequestDto {

    private String employeeId;
    private UUID categoryId;
    private UUID skillId;
    private UUID subskillId;
    private UUID proficiencyId;
    private String status;
}
