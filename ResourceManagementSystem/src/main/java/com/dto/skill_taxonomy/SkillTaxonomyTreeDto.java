package com.dto.skill_taxonomy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillTaxonomyTreeDto {
    
    private String id;
    private String name;
    private List<SkillTreeDto> skills;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkillTreeDto {
        private String id;
        private String name;
        private List<SubSkillTreeDto> subSkills;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubSkillTreeDto {
        private String id;
        private String name;
    }
}
