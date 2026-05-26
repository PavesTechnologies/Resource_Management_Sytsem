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
public class SubSkillMappingItemDto {

    private UUID subSkillId;
    private String subSkillName;
    private UUID proficiencyId;
    private String status;
}
