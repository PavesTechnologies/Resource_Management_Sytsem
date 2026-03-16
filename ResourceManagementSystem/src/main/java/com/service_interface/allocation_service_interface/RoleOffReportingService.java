package com.service_interface.allocation_service_interface;

import com.dto.allocation_dto.RoleOffReasonStatsDTO;
import com.dto.allocation_dto.RoleOffTrendDTO;
import com.dto.allocation_dto.ProjectRiskAnalysisDTO;
import com.entity_enums.allocation_enums.RoleOffReason;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface RoleOffReportingService {
    
    // Basic Statistics
    List<RoleOffReasonStatsDTO> getRoleOffReasonStatistics(LocalDate startDate, LocalDate endDate);
    Map<RoleOffReason, Long> getRoleOffReasonCounts();
    
    // Trend Analysis
    List<RoleOffTrendDTO> getRoleOffTrends(LocalDate startDate, LocalDate endDate);
    List<RoleOffTrendDTO> getRoleOffTrendsByReason(RoleOffReason reason, LocalDate startDate, LocalDate endDate);
    
    // Risk Analysis
    List<ProjectRiskAnalysisDTO> getProjectRiskAnalysis(LocalDate startDate, LocalDate endDate);
    List<ProjectRiskAnalysisDTO> getClientRiskAnalysis(LocalDate startDate, LocalDate endDate);
    Map<String, Object> getDeliveryRiskMetrics(LocalDate startDate, LocalDate endDate);
    
    // Performance Metrics
    List<Object[]> getPerformanceRelatedRoleOffs(LocalDate startDate, LocalDate endDate);
    Map<String, Double> getReasonDistribution(LocalDate startDate, LocalDate endDate);
    
    // Dashboard Data
    Map<String, Object> getDashboardData(LocalDate startDate, LocalDate endDate);
    Map<String, Object> getRiskDashboardData(LocalDate startDate, LocalDate endDate);
}
