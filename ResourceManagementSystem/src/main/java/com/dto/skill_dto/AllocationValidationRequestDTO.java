package com.dto.skill_dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class AllocationValidationRequestDTO {
    private Long resourceId;
    private List<UUID> requiredCertificationSkillIds;
}
