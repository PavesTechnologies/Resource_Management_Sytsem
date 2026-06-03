package com.dto.project_dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectGovernanceStatusDTO {
    private Long projectId;
    private boolean slaComplete;
    private boolean escalationComplete;
    private boolean complianceComplete;
    private boolean isReadyForDemand;
    private String message;
}
