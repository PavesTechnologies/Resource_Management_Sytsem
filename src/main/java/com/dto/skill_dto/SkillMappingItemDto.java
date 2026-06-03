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
public class SkillMappingItemDto {

    private UUID skillId;
    private String skillName;
    private UUID proficiencyId;
    private String status;
    private List<SubSkillMappingItemDto> subSkills;
}
