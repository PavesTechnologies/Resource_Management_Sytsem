package com.dto.client_dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClientProjectStatisticsDTO {
    // Existing fields
    private Long totalProjects;
    private Long activeProjects;
    private BigDecimal totalSpend;
    
    // New fields for enhanced statistics
    private Double satisfactionScore;        // 0-100 scale
    private Long pendingIssues;              // Count of active escalations
    private String overallHealth;             // EXCELLENT/GOOD/FAIR/POOR
    private HealthMetrics healthMetrics;      // Detailed breakdown
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class HealthMetrics {
        private Double onTimeDeliveryRate;    // Percentage
        private Double budgetPerformance;     // Percentage
        private Double resourceReadiness;     // Percentage
        private Double riskManagement;       // Percentage
        private Long highPriorityEscalations; // Count
        private Long delayedProjects;         // Count
        private Long highRiskProjects;        // Count
    }
}
