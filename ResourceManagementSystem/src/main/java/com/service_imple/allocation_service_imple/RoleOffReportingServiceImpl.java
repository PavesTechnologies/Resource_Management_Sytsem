package com.service_imple.allocation_service_imple;

import com.dto.allocation_dto.RoleOffReasonStatsDTO;
import com.dto.allocation_dto.RoleOffTrendDTO;
import com.dto.allocation_dto.ProjectRiskAnalysisDTO;
import com.entity.allocation_entities.RoleOffEvent;
import com.entity_enums.allocation_enums.RoleOffReason;
import com.repo.allocation_repo.RoleOffEventRepository;
import com.service_interface.allocation_service_interface.RoleOffReportingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleOffReportingServiceImpl implements RoleOffReportingService {

    private final RoleOffEventRepository roleOffRepo;

    @Override
    public List<RoleOffReasonStatsDTO> getRoleOffReasonStatistics(LocalDate startDate, LocalDate endDate) {
        List<Object[]> results = roleOffRepo.countByRoleOffReason();
        long total = results.stream().mapToLong(row -> (Long) row[1]).sum();
        
        return results.stream().map(row -> {
            RoleOffReason reason = (RoleOffReason) row[0];
            Long count = (Long) row[1];
            Double percentage = total > 0 ? (count * 100.0 / total) : 0.0;
            
            RoleOffReasonStatsDTO dto = new RoleOffReasonStatsDTO();
            dto.setReason(reason);
            dto.setCount(count);
            dto.setPercentage(percentage);
            dto.setDescription(getReasonDescription(reason));
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public List<RoleOffTrendDTO> getRoleOffTrends(LocalDate startDate, LocalDate endDate) {
        List<Object[]> results = roleOffRepo.getRoleOffTrendsByMonth(startDate, endDate);
        
        return results.stream().map(row -> {
            Integer year = (Integer) row[0];
            Integer month = (Integer) row[1];
            RoleOffReason reason = (RoleOffReason) row[2];
            Long count = (Long) row[3];
            
            RoleOffTrendDTO dto = new RoleOffTrendDTO();
            dto.setPeriod(YearMonth.of(year, month));
            dto.setReason(reason);
            dto.setCount(count);
            dto.setPeriodLabel(year + "-" + String.format("%02d", month));
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public List<ProjectRiskAnalysisDTO> getProjectRiskAnalysis(LocalDate startDate, LocalDate endDate) {
        List<Object[]> results = roleOffRepo.getRoleOffReasonsByProject(startDate, endDate);
        Map<String, Long> projectTotals = calculateProjectTotals(results);
        
        return results.stream().map(row -> {
            String projectName = (String) row[0];
            RoleOffReason reason = (RoleOffReason) row[1];
            Long count = (Long) row[2];
            
            ProjectRiskAnalysisDTO dto = new ProjectRiskAnalysisDTO();
            dto.setProjectName(projectName);
            dto.setReason(reason);
            dto.setCount(count);
            dto.setRiskScore(calculateRiskScore(reason, count, projectTotals.get(projectName)));
            dto.setRiskLevel(determineRiskLevel(dto.getRiskScore()));
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getDashboardData(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> dashboard = new HashMap<>();
        
        // Basic statistics
        dashboard.put("totalRoleOffs", roleOffRepo.count());
        dashboard.put("reasonStatistics", getRoleOffReasonStatistics(startDate, endDate));
        dashboard.put("trends", getRoleOffTrends(startDate, endDate));
        
        // Risk metrics
        dashboard.put("highRiskProjects", getHighRiskProjects(startDate, endDate));
        dashboard.put("performanceIssues", getPerformanceRelatedRoleOffs(startDate, endDate));
        
        // Recent activity
        dashboard.put("recentRoleOffs", getRecentRoleOffs(10));
        
        return dashboard;
    }

    @Override
    public Map<String, Object> getRiskDashboardData(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> riskDashboard = new HashMap<>();
        
        riskDashboard.put("projectRisks", getProjectRiskAnalysis(startDate, endDate));
        riskDashboard.put("clientRisks", getClientRiskAnalysis(startDate, endDate));
        riskDashboard.put("riskTrends", getRiskTrends(startDate, endDate));
        riskDashboard.put("riskMetrics", getDeliveryRiskMetrics(startDate, endDate));
        
        return riskDashboard;
    }

    // Helper methods
    private String getReasonDescription(RoleOffReason reason) {
        switch (reason) {
            case PROJECT_END: return "Resource role-off due to project completion or termination";
            case PERFORMANCE: return "Resource role-off due to performance issues";
            case ATTRITION: return "Resource role-off due to employee attrition/resignation";
            case CLIENT_REQUEST: return "Resource role-off due to client request or dissatisfaction";
            default: return reason.toString();
        }
    }

    private Double calculateRiskScore(RoleOffReason reason, Long count, Long totalProjectRoleOffs) {
        if (totalProjectRoleOffs == 0) return 0.0;
        
        double baseScore = (count * 100.0 / totalProjectRoleOffs);
        
        // Weight different reasons differently
        switch (reason) {
            case PERFORMANCE: return baseScore * 1.5; // Higher risk
            case CLIENT_REQUEST: return baseScore * 1.3;
            case ATTRITION: return baseScore * 1.2;
            case PROJECT_END: return baseScore * 0.5; // Lower risk (expected)
            default: return baseScore;
        }
    }

    private String determineRiskLevel(Double riskScore) {
        if (riskScore >= 70) return "HIGH";
        if (riskScore >= 40) return "MEDIUM";
        return "LOW";
    }

    private Map<String, Long> calculateProjectTotals(List<Object[]> results) {
        return results.stream()
            .collect(Collectors.groupingBy(
                row -> (String) row[0],
                Collectors.summingLong(row -> (Long) row[2])
            ));
    }

    private List<Map<String, Object>> getHighRiskProjects(LocalDate startDate, LocalDate endDate) {
        return getProjectRiskAnalysis(startDate, endDate).stream()
            .filter(project -> "HIGH".equals(project.getRiskLevel()))
            .limit(10)
            .collect(Collectors.toList());
    }

    private List<RoleOffEvent> getRecentRoleOffs(int limit) {
        return roleOffRepo.findAll().stream()
            .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
            .limit(limit)
            .collect(Collectors.toList());
    }

    // Implement other interface methods...
    @Override
    public Map<RoleOffReason, Long> getRoleOffReasonCounts() {
        return roleOffRepo.countByRoleOffReason().stream()
            .collect(Collectors.toMap(
                row -> (RoleOffReason) row[0],
                row -> (Long) row[1]
            ));
    }

    @Override
    public List<RoleOffTrendDTO> getRoleOffTrendsByReason(RoleOffReason reason, LocalDate startDate, LocalDate endDate) {
        return getRoleOffTrends(startDate, endDate).stream()
            .filter(trend -> trend.getReason().equals(reason))
            .collect(Collectors.toList());
    }

    @Override
    public List<ProjectRiskAnalysisDTO> getClientRiskAnalysis(LocalDate startDate, LocalDate endDate) {
        List<Object[]> results = roleOffRepo.getRoleOffReasonsByClient(startDate, endDate);
        // Similar implementation to project risk analysis
        return Collections.emptyList(); // Placeholder
    }

    @Override
    public Map<String, Object> getDeliveryRiskMetrics(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> metrics = new HashMap<>();
        
        // Calculate various risk metrics
        long totalRoleOffs = roleOffRepo.count();
        long performanceIssues = getPerformanceRelatedRoleOffs(startDate, endDate).size();
        
        metrics.put("totalRoleOffs", totalRoleOffs);
        metrics.put("performanceIssueRate", totalRoleOffs > 0 ? (performanceIssues * 100.0 / totalRoleOffs) : 0.0);
        metrics.put("riskTrend", calculateRiskTrend(startDate, endDate));
        
        return metrics;
    }

    @Override
    public List<Object[]> getPerformanceRelatedRoleOffs(LocalDate startDate, LocalDate endDate) {
        return roleOffRepo.getPerformanceRelatedRoleOffs(startDate, endDate).stream()
            .map(event -> new Object[]{event.getRoleOffReason(), event})
            .collect(Collectors.toList());
    }

    @Override
    public Map<String, Double> getReasonDistribution(LocalDate startDate, LocalDate endDate) {
        List<RoleOffReasonStatsDTO> stats = getRoleOffReasonStatistics(startDate, endDate);
        return stats.stream()
            .collect(Collectors.toMap(
                stat -> stat.getReason().toString(),
                RoleOffReasonStatsDTO::getPercentage
            ));
    }

    private List<Map<String, Object>> getRiskTrends(LocalDate startDate, LocalDate endDate) {
        // Implement risk trend calculation
        return Collections.emptyList(); // Placeholder
    }
}
