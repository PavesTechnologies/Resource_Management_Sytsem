package com.dto.skill_dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResourceSkillBulkRequestDTO {
    
    @NotNull(message = "Resource ID is required")
    private Long resourceId;
    
    @Valid
    @NotNull(message = "Skills list is required")
    private List<SkillWithSubSkillDTO> skills;
}
