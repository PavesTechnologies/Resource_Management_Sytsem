package com.dto.skill_dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResourceSkillResponseDTO {

    private UUID resourceSkillId;
    private UUID categoryId;
    private String categoryName;
    private UUID skillId;
    private String skillName;
    private String proficiency;
    private Boolean active;

    @Builder.Default
    private List<SubSkillDTO> subSkills = new java.util.ArrayList<>();

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SubSkillDTO {
        private UUID subSkillId;
        private String subSkillName;
        private String proficiency;
        private Boolean active;
    }
}
