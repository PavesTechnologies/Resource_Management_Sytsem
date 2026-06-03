package com.dto.skill_dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryRequestDto {
    private UUID id; // Null for creation, present for update
    private String name;
    private String description;
    private Boolean active; // Use Boolean to allow null for partial updates or default
    private List<SkillRequestDto> skills;
}
