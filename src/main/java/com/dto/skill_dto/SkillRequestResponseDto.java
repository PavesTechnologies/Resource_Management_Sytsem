package com.dto.skill_dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillRequestResponseDto {

    private UUID id;
    private String resourceId;
    private String resourceName;
    private String categoryName;
    private String skillName;
    private String subskillName;
    private String proficiency;
    private String requestStatus;
    private LocalDateTime requestedAt;
    private LocalDateTime approvedAt;
    private String approvedBy;
    private String remarks;
}
