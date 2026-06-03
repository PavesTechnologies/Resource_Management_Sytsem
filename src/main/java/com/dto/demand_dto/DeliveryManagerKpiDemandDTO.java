package com.dto.demand_dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryManagerKpiDemandDTO {
    
    // Delivery Manager Information
    private Long deliveryManagerId;
    private String deliveryManagerName;
    
    // KPI Summary
    private DemandKpiDTO kpiSummary;
    
    // Project Details
    private List<ProjectDemandSummaryDTO> projectSummaries;
    
    // Overall Statistics
    private Integer totalProjects;
    private Integer totalDemands;
    private Integer activeDemands;
    private Integer approvedDemands;
    private Integer pendingDemands;
    private Integer slaAtRiskDemands;
    private Integer slaBreachedDemands;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProjectDemandSummaryDTO {
        
        // Project Information
        private Long projectId;
        private String projectName;
        private UUID clientId;
        private String clientName;
        
        // Project Status
        private String projectStatus;
        private String lifecycleStage;
        private String primaryLocation;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        
        // Demand Summary for this Project
        private DemandKpiDTO projectKpi;
        
        // Individual Demand Details
        private List<DemandDetailDTO> demands;
        
        // Project-specific metrics
        private Integer totalResourcesRequired;
        private Integer allocatedResources;
        private Integer pendingResources;
        private Double allocationPercentage;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DemandDetailDTO {
        
        // Basic Demand Information
        private UUID demandId;
        private String demandName;
        private String demandStatus;
        private String demandType;
        private String demandPriority;
        private String deliveryModel;
        
        // Demand Dates
        private LocalDate demandStartDate;
        private LocalDate demandEndDate;
        
        // Resource Requirements
        private Integer resourcesRequired;
        private Integer allocationPercentage;
        private Double minExp;
        
        // SLA Information
        private UUID demandSlaId;
        private String slaType;
        private Integer slaDurationDays;
        private Integer warningThresholdDays;
        private LocalDateTime slaCreatedAt;
        private LocalDateTime slaDueAt;
        private Boolean slaBreached;
        private Integer overdueDays;
        private Integer remainingDays;
        
        // Role Information
        private UUID roleId;
        private String roleName;
        
        // Priority Score (calculated)
        private Integer priorityScore;
        
        // Additional Information
        private String demandJustification;
        private Boolean requiresAdditionalApproval;
        private LocalDateTime createdAt;
    }
}
