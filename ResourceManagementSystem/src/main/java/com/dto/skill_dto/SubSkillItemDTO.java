package com.dto.skill_dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubSkillItemDTO {
    
    private String name;
    private String description;
    private Boolean isActive;
}
