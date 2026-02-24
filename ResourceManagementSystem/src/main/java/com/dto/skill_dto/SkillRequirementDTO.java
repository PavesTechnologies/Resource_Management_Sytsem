package com.dto.skill_dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SkillRequirementDTO {
    private UUID skillId;
    private UUID requiredProficiencyId;

}
