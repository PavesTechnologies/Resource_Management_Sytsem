package com.dto.project_dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectEligibilityDTO {
    private boolean isEligible;
    private String reason;
}
