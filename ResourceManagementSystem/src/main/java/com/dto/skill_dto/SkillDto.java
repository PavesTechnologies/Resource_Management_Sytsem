package com.dto.skill_dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SkillDto {
    private UUID id;
    private String name;
    private String description;
    private boolean active;
}
