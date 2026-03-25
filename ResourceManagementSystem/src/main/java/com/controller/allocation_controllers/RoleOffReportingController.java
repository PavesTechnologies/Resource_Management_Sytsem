package com.controller.allocation_controllers;

import com.dto.centralised_dto.UserDTO;
import com.dto.roleoff_dto.RoleOffReportDTO;
import com.security.CurrentUser;
import com.service_interface.allocation_service_interface.RoleOffReportingService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports/role-off")
@RequiredArgsConstructor
// @PreAuthorize("hasAnyRole('RESOURCE_MANAGER', 'ADMIN', 'REPORT_MANAGER')")
public class RoleOffReportingController {

    private final RoleOffReportingService reportingService;

    // Single Comprehensive Dashboard Endpoint
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getRoleOffDashboard(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @CurrentUser UserDTO userDTO) {
        
        Map<String, Object> dashboard = reportingService.getDashboardData(startDate, endDate);
        return ResponseEntity.ok(dashboard);
    }

    // NEW: Multi-Dimensional Reporting Endpoints (2 DTOs only)
    @PostMapping("/filtered")
    public ResponseEntity<RoleOffReportDTO> getMultiDimensionalReport(
            @RequestBody RoleOffReportDTO filter,
            @CurrentUser UserDTO userDTO) {

        RoleOffReportDTO report = reportingService.getMultiDimensionalReport(filter);
        return ResponseEntity.ok(report);
    }

    // NEW: Get all role-offs (for testing/data verification)
    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getAllRoleOffs(@CurrentUser UserDTO userDTO) {
        Map<String, Object> allData = new HashMap<>();

        // Get all role-offs without filtering
        List<RoleOffReportDTO> allEvents = reportingService.getAllRoleOffEvents();
        allData.put("totalRoleOffs", (long) allEvents.size());
        allData.put("allEvents", allEvents);

        // Reason breakdown for all data
        Map<String, Long> reasonCounts = allEvents.stream()
            .filter(event -> event.getRoleOffReason() != null)
            .collect(java.util.stream.Collectors.groupingBy(
                event -> event.getRoleOffReason().toString(),
                java.util.stream.Collectors.counting()
            ));
        allData.put("reasonBreakdown", reasonCounts);

        return ResponseEntity.ok(allData);
    }

    @PostMapping("/events")
    public ResponseEntity<List<RoleOffReportDTO>> getRoleOffEventsByFilter(
            @RequestBody RoleOffReportDTO filter,
            @CurrentUser UserDTO userDTO) {

        List<RoleOffReportDTO> events = reportingService.getRoleOffEventsByFilter(filter);
        return ResponseEntity.ok(events);
    }

    @PostMapping("/count")
    public ResponseEntity<Long> getRoleOffCountByFilter(
            @RequestBody RoleOffReportDTO filter,
            @CurrentUser UserDTO userDTO) {

        Long count = reportingService.getRoleOffCountByFilter(filter);
        return ResponseEntity.ok(count);
    }

    // NEW: Export Endpoint
    @PostMapping("/export/csv")
    public ResponseEntity<byte[]> exportToCsv(
            @RequestBody RoleOffReportDTO filter,
            @CurrentUser UserDTO userDTO) {

        return reportingService.exportToCsv(filter);
    }
}
