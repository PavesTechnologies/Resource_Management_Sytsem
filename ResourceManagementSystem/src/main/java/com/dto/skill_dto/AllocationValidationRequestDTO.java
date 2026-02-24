package com.dto.skill_dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AllocationValidationRequestDTO {
    private Long resourceId;
    private List<SkillRequirementDTO> skillRequirements;
    private List<UUID> requiredCertificationSkillIds;
}
