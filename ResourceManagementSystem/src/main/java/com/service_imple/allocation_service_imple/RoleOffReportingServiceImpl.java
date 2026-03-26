package com.service_imple.allocation_service_imple;

import com.dto.allocation_dto.RoleOffReasonStatsDTO;
import com.dto.allocation_dto.RoleOffTrendDTO;
import com.dto.allocation_dto.ProjectRiskAnalysisDTO;
import com.entity.roleoff_entities.RoleOffEvent;
import com.entity_enums.roleoff_enums.RoleOffReason;
import com.dto.roleoff_dto.RoleOffReportDTO;
import com.dto.roleoff_dto.RoleOffExportDTO;
import com.repo.roleoff_repo.RoleOffEventRepository;
import com.service_interface.allocation_service_interface.RoleOffReportingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
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
            .map(this::convertProjectRiskToMap)
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

    private Double calculateRiskTrend(LocalDate startDate, LocalDate endDate) {
        // Simple implementation: compare role-offs in current period vs previous period
        LocalDate previousStart = startDate.minusMonths(1);
        LocalDate previousEnd = endDate.minusMonths(1);
        
        long currentPeriodCount = roleOffRepo.countRoleOffsByDateRange(startDate, endDate);
        long previousPeriodCount = roleOffRepo.countRoleOffsByDateRange(previousStart, previousEnd);
        
        if (previousPeriodCount == 0) {
            return currentPeriodCount > 0 ? 100.0 : 0.0;
        }
        
        return ((double)(currentPeriodCount - previousPeriodCount) / previousPeriodCount) * 100.0;
    }

    private List<Map<String, Object>> getRiskTrends(LocalDate startDate, LocalDate endDate) {
        // Implement risk trend calculation
        return Collections.emptyList(); // Placeholder
    }

    private Map<String, Object> convertProjectRiskToMap(ProjectRiskAnalysisDTO project) {
        Map<String, Object> map = new HashMap<>();
        map.put("projectName", project.getProjectName());
        map.put("projectId", project.getProjectId());
        map.put("reason", project.getReason());
        map.put("count", project.getCount());
        map.put("riskScore", project.getRiskScore());
        map.put("riskLevel", project.getRiskLevel());
        return map;
    }

    // NEW: Multi-Dimensional Reporting Implementation (2 DTOs only)
    @Override
    public List<RoleOffReportDTO> getRoleOffEventsByFilter(RoleOffReportDTO filter) {
        try {
            System.out.println("DEBUG: getRoleOffEventsByFilter called with:");
            System.out.println("  startDate: " + filter.getStartDate());
            System.out.println("  endDate: " + filter.getEndDate());
            System.out.println("  projectIds: " + filter.getProjectIds());
            System.out.println("  reasons: " + filter.getReasons());
            System.out.println("  clientIds: " + filter.getClientIds());

            List<RoleOffReason> reasonEnums = filter.getReasonsAsEnum();
            System.out.println("  converted reasonEnums: " + reasonEnums);

            List<RoleOffEvent> events = roleOffRepo.findFilteredRoleOffs(
                filter.getStartDate(),
                filter.getEndDate(),
                filter.getProjectIds(),
                reasonEnums,
                filter.getClientIds()
            );

            System.out.println("  repository returned " + (events != null ? events.size() : "null") + " events");

            List<RoleOffReportDTO> dtos = events.stream()
                .map(RoleOffReportDTO::fromEntity)
                .collect(Collectors.toList());
            System.out.println("  converted to " + dtos.size() + " DTOs");

            return dtos;
        } catch (Exception e) {
            System.err.println("ERROR in getRoleOffEventsByFilter: " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    @Override
    public Long getRoleOffCountByFilter(RoleOffReportDTO filter) {
        try {
            List<RoleOffEvent> events = roleOffRepo.findFilteredRoleOffs(
                filter.getStartDate(),
                filter.getEndDate(),
                filter.getProjectIds(),
                filter.getReasonsAsEnum(),
                filter.getClientIds()
            );
            return (long) events.size();
        } catch (Exception e) {
            System.err.println("Error in getRoleOffCountByFilter: " + e.getMessage());
            return 0L;
        }
    }

    @Override
    public RoleOffReportDTO getMultiDimensionalReport(RoleOffReportDTO filter) {
        try {
            System.out.println("DEBUG: Starting getMultiDimensionalReport with filter: " + filter);

            // Get filtered events
            System.out.println("DEBUG: Calling getRoleOffEventsByFilter");
            List<RoleOffReportDTO> events = getRoleOffEventsByFilter(filter);
            System.out.println("DEBUG: Got events count: " + (events != null ? events.size() : "null"));

            Long totalCount = getRoleOffCountByFilter(filter);
            System.out.println("DEBUG: Total count: " + totalCount);

            // Reason breakdown
            Map<String, Long> reasonCounts = new HashMap<>();
            if (events != null) {
                reasonCounts = events.stream()
                    .filter(event -> event != null && event.getRoleOffReason() != null)
                    .collect(Collectors.groupingBy(
                        event -> event.getRoleOffReason().toString(),
                        Collectors.counting()
                    ));
            }

            // NEW: Risk Analysis
            List<RoleOffReportDTO.RiskAlert> riskAlerts = analyzeRiskPatterns(events);
            Map<String, Object> riskMetrics = calculateRiskMetrics(events);
            Boolean hasHighRiskPatterns = hasHighRiskPatterns(events);

            // Return report with risk analysis
            return RoleOffReportDTO.builder()
                .totalRoleOffs(totalCount)
                .filteredEvents(events)
                .reasonBreakdown(reasonCounts)
                .riskAlerts(riskAlerts)
                .riskMetrics(riskMetrics)
                .hasHighRiskPatterns(hasHighRiskPatterns)
                .build();

        } catch (Exception e) {
            System.err.println("ERROR in getMultiDimensionalReport: " + e.getMessage());
            e.printStackTrace();

            // Return error DTO with essential fields only
            return RoleOffReportDTO.builder()
                .totalRoleOffs(0L)
                .filteredEvents(Collections.emptyList())
                .reasonBreakdown(Collections.emptyMap())
                .riskAlerts(Collections.emptyList())
                .riskMetrics(Collections.emptyMap())
                .hasHighRiskPatterns(false)
                .build();
        }
    }

    @Override
    public List<RoleOffReportDTO> getAllRoleOffEvents() {
        try {
            List<RoleOffEvent> events = roleOffRepo.findAll();
            return events.stream()
                .map(RoleOffReportDTO::fromEntity)
                .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error in getAllRoleOffEvents: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    // NEW: Risk Analysis Implementation
    @Override
    public List<RoleOffReportDTO.RiskAlert> analyzeRiskPatterns(List<RoleOffReportDTO> events) {
        List<RoleOffReportDTO.RiskAlert> alerts = new ArrayList<>();

        if (events == null || events.isEmpty()) {
            return alerts;
        }

        // 1. High Frequency Project Risk
        Map<Long, Long> projectCounts = events.stream()
            .filter(e -> e.getProjectId() != null)
            .collect(Collectors.groupingBy(
                RoleOffReportDTO::getProjectId,
                Collectors.counting()
            ));

        projectCounts.entrySet().stream()
            .filter(entry -> entry.getValue() >= 3) // 3+ role-offs in same project
            .forEach(entry -> {
                List<String> affectedProjects = events.stream()
                    .filter(e -> entry.getKey().equals(e.getProjectId()))
                    .map(RoleOffReportDTO::getProjectName)
                    .distinct()
                    .collect(Collectors.toList());

                alerts.add(RoleOffReportDTO.RiskAlert.builder()
                    .type("HIGH_FREQUENCY_PROJECT")
                    .severity("HIGH")
                    .description("Project has " + entry.getValue() + " role-offs, indicating potential project issues")
                    .count(entry.getValue().intValue())
                    .affectedProjects(affectedProjects)
                    .recommendation("Review project health, resource allocation, and client satisfaction")
                    .build());
            });

        // 2. Performance Issue Risk
        long performanceCount = events.stream()
            .filter(e -> e.getRoleOffReason() == RoleOffReason.PERFORMANCE)
            .count();

        if (performanceCount >= 2) {
            List<String> affectedResources = events.stream()
                .filter(e -> e.getRoleOffReason() == RoleOffReason.PERFORMANCE)
                .map(RoleOffReportDTO::getResourceName)
                .distinct()
                .collect(Collectors.toList());

            alerts.add(RoleOffReportDTO.RiskAlert.builder()
                .type("PERFORMANCE_ISSUES")
                .severity("HIGH")
                .description(performanceCount + " role-offs due to performance issues")
                .count((int) performanceCount)
                .affectedResources(affectedResources)
                .recommendation("Review performance management processes and provide additional training/support")
                .build());
        }

        // 3. Client Request Risk (Budget/Contract Issues)
        long clientRequestCount = events.stream()
            .filter(e -> e.getRoleOffReason() == RoleOffReason.CLIENT_REQUEST)
            .count();

        if (clientRequestCount >= 2) {
            List<String> affectedProjects = events.stream()
                .filter(e -> e.getRoleOffReason() == RoleOffReason.CLIENT_REQUEST)
                .map(RoleOffReportDTO::getProjectName)
                .distinct()
                .collect(Collectors.toList());

            alerts.add(RoleOffReportDTO.RiskAlert.builder()
                .type("CLIENT_CONCERNS")
                .severity("MEDIUM")
                .description(clientRequestCount + " role-offs requested by client")
                .count((int) clientRequestCount)
                .affectedProjects(affectedProjects)
                .recommendation("Engage with clients to understand concerns and improve service delivery")
                .build());
        }

        // 4. Concentrated Time Period Risk
        Map<YearMonth, Long> monthlyCounts = events.stream()
            .filter(e -> e.getEffectiveRoleOffDate() != null)
            .collect(Collectors.groupingBy(
                e -> YearMonth.from(e.getEffectiveRoleOffDate()),
                Collectors.counting()
            ));

        monthlyCounts.entrySet().stream()
            .filter(entry -> entry.getValue() >= 5) // 5+ role-offs in same month
            .forEach(entry -> {
                alerts.add(RoleOffReportDTO.RiskAlert.builder()
                    .type("CONCENTRATED_ROLE_OFFS")
                    .severity("MEDIUM")
                    .description(entry.getValue() + " role-offs in " + entry.getKey() + ", indicating systemic issues")
                    .count(entry.getValue().intValue())
                    .recommendation("Investigate organizational changes, policy updates, or external factors")
                    .build());
            });

        return alerts;
    }

    @Override
    public Map<String, Object> calculateRiskMetrics(List<RoleOffReportDTO> events) {
        Map<String, Object> metrics = new HashMap<>();

        if (events == null || events.isEmpty()) {
            return metrics;
        }

        // Risk Score Calculation
        double riskScore = 0.0;

        // High frequency projects (30 points each)
        long highFreqProjects = events.stream()
            .collect(Collectors.groupingBy(RoleOffReportDTO::getProjectId, Collectors.counting()))
            .values().stream()
            .filter(count -> count >= 3)
            .count();
        riskScore += highFreqProjects * 30;

        // Performance issues (25 points each)
        long performanceIssues = events.stream()
            .filter(e -> e.getRoleOffReason() == RoleOffReason.PERFORMANCE)
            .count();
        riskScore += performanceIssues * 25;

        // Client requests (15 points each)
        long clientRequests = events.stream()
            .filter(e -> e.getRoleOffReason() == RoleOffReason.CLIENT_REQUEST)
            .count();
        riskScore += clientRequests * 15;

        // Concentrated periods (20 points each)
        long concentratedPeriods = events.stream()
            .filter(e -> e.getEffectiveRoleOffDate() != null)
            .collect(Collectors.groupingBy(e -> YearMonth.from(e.getEffectiveRoleOffDate()), Collectors.counting()))
            .values().stream()
            .filter(count -> count >= 5)
            .count();
        riskScore += concentratedPeriods * 20;

        // Risk Level Classification
        String riskLevel = "LOW";
        if (riskScore >= 80) riskLevel = "CRITICAL";
        else if (riskScore >= 60) riskLevel = "HIGH";
        else if (riskScore >= 30) riskLevel = "MEDIUM";

        metrics.put("riskScore", Math.round(riskScore));
        metrics.put("riskLevel", riskLevel);
        metrics.put("highFrequencyProjects", highFreqProjects);
        metrics.put("performanceIssues", performanceIssues);
        metrics.put("clientRequests", clientRequests);
        metrics.put("concentratedPeriods", concentratedPeriods);
        metrics.put("totalEvents", events.size());

        return metrics;
    }

    @Override
    public Boolean hasHighRiskPatterns(List<RoleOffReportDTO> events) {
        if (events == null || events.isEmpty()) {
            return false;
        }

        // Check for any high-risk patterns
        return events.stream()
            .collect(Collectors.groupingBy(RoleOffReportDTO::getProjectId, Collectors.counting()))
            .values().stream()
            .anyMatch(count -> count >= 3) // 3+ role-offs in same project
            || events.stream().filter(e -> e.getRoleOffReason() == RoleOffReason.PERFORMANCE).count() >= 2
            || events.stream().filter(e -> e.getRoleOffReason() == RoleOffReason.CLIENT_REQUEST).count() >= 3;
    }

    // NEW: Export Implementation
    @Override
    public List<RoleOffExportDTO> exportRoleOffData(RoleOffReportDTO filter) {
        try {
            // Get filtered events using existing logic
            List<RoleOffReportDTO> events = getRoleOffEventsByFilter(filter);

            // Convert to export format
            return events.stream()
                .map(RoleOffExportDTO::fromReportDTO)
                .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error in exportRoleOffData: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public ResponseEntity<byte[]> exportToCsv(RoleOffReportDTO filter) {
        try {
            List<RoleOffExportDTO> exportData = exportRoleOffData(filter);

            // Generate CSV content
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PrintWriter writer = new PrintWriter(outputStream, true, StandardCharsets.UTF_8);

            // Write headers
            String[] headers = RoleOffExportDTO.getCsvHeaders();
            writer.println(String.join(",", headers));

            // Write data rows
            for (RoleOffExportDTO row : exportData) {
                String[] rowData = row.getCsvRow();
                // Escape commas and quotes in data
                for (int i = 0; i < rowData.length; i++) {
                    if (rowData[i].contains(",") || rowData[i].contains("\"")) {
                        rowData[i] = "\"" + rowData[i].replace("\"", "\"\"") + "\"";
                    }
                }
                writer.println(String.join(",", rowData));
            }

            writer.flush();
            writer.close();

            // Create filename with timestamp
            String filename = "role-off-report-" + LocalDate.now().toString() + ".csv";

            // Create response with proper headers
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(MediaType.parseMediaType("text/csv"));
            responseHeaders.setContentDispositionFormData("attachment", filename);
            responseHeaders.setContentLength(outputStream.size());

            return ResponseEntity.ok()
                .headers(responseHeaders)
                .body(outputStream.toByteArray());

        } catch (Exception e) {
            System.err.println("Error generating CSV export: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}
