package com.dto.skill_dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SkillTaxonomyRequestDto {
    private List<CategoryRequestDto> categories;
}
