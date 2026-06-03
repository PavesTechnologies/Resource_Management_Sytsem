package com.dto.skill_dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResourceSkillProfileResponseDTO {
    private UUID resourceSkillId;
    private String category;
    private String skill;
    private String skillProficiency;
    private String skillProficiencyCode;
    private List<SubSkillProficiencyDTO> subSkills;
    private LocalDate lastUsedDate;
    
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SubSkillProficiencyDTO {
        private UUID resourceSubSkillId;
        private String subSkill;
        private String proficiency;
        private String proficiencyCode;
    }
}
