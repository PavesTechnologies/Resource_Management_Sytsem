package com.dto.project_dto;

import com.entity_enums.client_enums.ComplianceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProjectComplianceResponseDTO {
    private UUID projectComplianceId;
    private Long projectId;
    private ComplianceType complianceType;
    private String requirementName;
    private Boolean mandatoryFlag;
    private Boolean isInherited;
    private Boolean activeFlag;
    private UUID clientComplianceId;
}
