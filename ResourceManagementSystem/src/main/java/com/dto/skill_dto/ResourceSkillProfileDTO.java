package com.dto.skill_dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder
public class ResourceSkillProfileDTO {
    private String category;
    private String skill;
    private String proficiency;
    private String proficiencyCode;
    private LocalDate lastUsedDate;
}
