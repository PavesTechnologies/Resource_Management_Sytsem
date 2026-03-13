package com.controller.allocation_controllers;

import com.dto.UserDTO;
import com.dto.allocation_dto.RoleOffReasonStatsDTO;
import com.dto.allocation_dto.RoleOffTrendDTO;
import com.dto.allocation_dto.ProjectRiskAnalysisDTO;
import com.entity_enums.allocation_enums.RoleOffReason;
import com.security.CurrentUser;
import com.service_interface.allocation_service_interface.RoleOffReportingService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reports/role-off")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('RESOURCE_MANAGER', 'ADMIN', 'REPORT_MANAGER')")
public class RoleOffReportingController {

    private final RoleOffReportingService reportingService;

    // Basic Statistics Endpoints
    @GetMapping("/statistics")
    public ResponseEntity<List<RoleOffReasonStatsDTO>> getRoleOffStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @CurrentUser UserDTO userDTO) {
        
        List<RoleOffReasonStatsDTO> statistics = reportingService.getRoleOffReasonStatistics(startDate, endDate);
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/counts")
    public ResponseEntity<Map<String, Long>> getRoleOffCounts(@CurrentUser UserDTO userDTO) {
        Map<String, Long> counts = reportingService.getRoleOffReasonCounts().entrySet().stream()
            .collect(Collectors.toMap(
                entry -> entry.getKey().toString(),
                Map.Entry::getValue
            ));
        return ResponseEntity.ok(counts);
    }

    // Trend Analysis Endpoints
    @GetMapping("/trends")
    public ResponseEntity<List<RoleOffTrendDTO>> getRoleOffTrends(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @CurrentUser UserDTO userDTO) {
        
        List<RoleOffTrendDTO> trends = reportingService.getRoleOffTrends(startDate, endDate);
        return ResponseEntity.ok(trends);
    }

    @GetMapping("/trends/{reason}")
    public ResponseEntity<List<RoleOffTrendDTO>> getRoleOffTrendsByReason(
            @PathVariable String reason,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @CurrentUser UserDTO userDTO) {
        
        try {
            RoleOffReason reasonEnum = RoleOffReason.valueOf(reason.toUpperCase());
            List<RoleOffTrendDTO> trends = reportingService.getRoleOffTrendsByReason(reasonEnum, startDate, endDate);
            return ResponseEntity.ok(trends);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // Risk Analysis Endpoints
    @GetMapping("/risk/projects")
    public ResponseEntity<List<ProjectRiskAnalysisDTO>> getProjectRiskAnalysis(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @CurrentUser UserDTO userDTO) {
        
        List<ProjectRiskAnalysisDTO> risks = reportingService.getProjectRiskAnalysis(startDate, endDate);
        return ResponseEntity.ok(risks);
    }

    @GetMapping("/risk/clients")
    public ResponseEntity<List<ProjectRiskAnalysisDTO>> getClientRiskAnalysis(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @CurrentUser UserDTO userDTO) {
        
        List<ProjectRiskAnalysisDTO> risks = reportingService.getClientRiskAnalysis(startDate, endDate);
        return ResponseEntity.ok(risks);
    }

    // Performance Metrics Endpoints
    @GetMapping("/performance")
    public ResponseEntity<List<Object[]>> getPerformanceRelatedRoleOffs(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @CurrentUser UserDTO userDTO) {
        
        List<Object[]> performanceIssues = reportingService.getPerformanceRelatedRoleOffs(startDate, endDate);
        return ResponseEntity.ok(performanceIssues);
    }

    @GetMapping("/distribution")
    public ResponseEntity<Map<String, Double>> getReasonDistribution(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @CurrentUser UserDTO userDTO) {
        
        Map<String, Double> distribution = reportingService.getReasonDistribution(startDate, endDate);
        return ResponseEntity.ok(distribution);
    }

    // Dashboard Endpoints
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardData(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @CurrentUser UserDTO userDTO) {
        
        Map<String, Object> dashboard = reportingService.getDashboardData(startDate, endDate);
        return ResponseEntity.ok(dashboard);
    }

    @GetMapping("/dashboard/risk")
    public ResponseEntity<Map<String, Object>> getRiskDashboardData(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @CurrentUser UserDTO userDTO) {
        
        Map<String, Object> riskDashboard = reportingService.getRiskDashboardData(startDate, endDate);
        return ResponseEntity.ok(riskDashboard);
    }

    // Risk Metrics Endpoint
    @GetMapping("/risk/metrics")
    public ResponseEntity<Map<String, Object>> getDeliveryRiskMetrics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @CurrentUser UserDTO userDTO) {
        
        Map<String, Object> metrics = reportingService.getDeliveryRiskMetrics(startDate, endDate);
        return ResponseEntity.ok(metrics);
    }

    // Export Endpoints
    @GetMapping("/export/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @CurrentUser UserDTO userDTO) {
        
        // Implementation for CSV/Excel export
        return ResponseEntity.ok().build();
    }

    @GetMapping("/export/trends")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportTrends(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @CurrentUser UserDTO userDTO) {
        
        // Implementation for CSV/Excel export
        return ResponseEntity.ok().build();
    }
}
