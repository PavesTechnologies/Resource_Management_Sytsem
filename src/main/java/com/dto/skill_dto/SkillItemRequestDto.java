package com.dto.skill_dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillItemRequestDto {

    private String skillName;
    private UUID proficiencyId;
    private String status;
    private List<SubSkillRequestItemDto> subSkills;
}
