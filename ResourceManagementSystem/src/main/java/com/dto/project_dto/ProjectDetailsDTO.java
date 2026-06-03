package com.dto.project_dto;

import com.entity_enums.centralised_enums.DeliveryModel;
import com.entity_enums.centralised_enums.RiskLevel;
import com.entity_enums.project_enums.ProjectStage;
import com.entity_enums.project_enums.ProjectStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ProjectDetailsDTO {
    private Long pmsProjectId;
    private String projectName;
    private UUID clientId;
    private String clientName;
    private Long projectManagerId;
    private DeliveryModel deliveryModel;
    private String primaryLocation;
    private RiskLevel riskLevel;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private BigDecimal projectBudget;
    private ProjectStatus projectStatus;
    private ProjectStage lifecycleStage;
    private boolean hasOverlap;
}
