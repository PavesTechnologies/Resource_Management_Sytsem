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
public class EmployeeSkillResponseDto {

    private UUID id;
    private String employeeId;
    private String categoryName;
    private String skillName;
    private String subskillName;
    private String proficiency;
    private String status;
}
