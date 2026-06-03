package com.dto.skill_dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubSkillResponseDto {
    private UUID id;
    private String name;
    private String description;
    private boolean active;
    private String operation; // Added to resolve the error
}
