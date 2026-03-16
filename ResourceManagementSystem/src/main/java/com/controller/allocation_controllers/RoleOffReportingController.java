package com.controller.allocation_controllers;

import com.dto.UserDTO;
import com.security.CurrentUser;
import com.service_interface.allocation_service_interface.RoleOffReportingService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/reports/role-off")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('RESOURCE_MANAGER', 'ADMIN', 'REPORT_MANAGER')")
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
}
