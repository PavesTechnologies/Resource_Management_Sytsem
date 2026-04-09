package com.dto.skill_dto;

import jakarta.validation.constraints.NotNull;
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
public class ResourceSkillRequestDTO {
    
    @NotNull(message = "Resource ID is required")
    private Long resourceId;
    
    @NotNull(message = "Skill ID is required")
    private UUID skillId;
    
    @NotNull(message = "Proficiency ID is required")
    private UUID proficiencyId;
    
    private LocalDate lastUsedDate;
    
    private Boolean activeFlag;
    
    private List<SubSkillUpdateDTO> subSkills;
    
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SubSkillUpdateDTO {
        @NotNull(message = "SubSkill ID is required")
        private UUID subSkillId;
        
        @NotNull(message = "Proficiency ID is required")
        private UUID proficiencyId;
        
        private LocalDate lastUsedDate;
        
        private Boolean activeFlag;
    }
}
