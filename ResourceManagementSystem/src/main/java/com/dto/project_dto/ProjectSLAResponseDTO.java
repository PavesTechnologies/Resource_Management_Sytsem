package com.dto.project_dto;


import com.entity_enums.client_enums.SLAType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProjectSLAResponseDTO {
    private UUID projectSlaId;
    private Long projectId;
    private SLAType slaType;

    private Integer slaDurationDays;
    private Integer warningThresholdDays;

    private Boolean isInherited;
    private Boolean activeFlag;

    private UUID clientSlaId;
}
