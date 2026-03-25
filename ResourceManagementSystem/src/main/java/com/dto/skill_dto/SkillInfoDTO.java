package com.dto.skill_dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillInfoDTO {
    
    private String skillName;
    private String proficiencyLevel;
    private LocalDate lastUsedDate;
    
    public static SkillInfoDTO from(String skillName, String proficiencyLevel, LocalDate lastUsedDate) {
        return SkillInfoDTO.builder()
                .skillName(skillName)
                .proficiencyLevel(proficiencyLevel)
                .lastUsedDate(lastUsedDate)
                .build();
    }
}
