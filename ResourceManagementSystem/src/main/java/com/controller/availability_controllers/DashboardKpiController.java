package com.controller.availability_controllers;

import com.dto.DashboardKpiDTO;
import com.service_interface.kpi_service.DashboardKpiService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.global_exception_handler.ProjectExceptionHandler;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/rms")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Dashboard KPI", description = "Dashboard KPI metrics for workforce availability")
public class DashboardKpiController {

    private final DashboardKpiService dashboardKpiService;

    @GetMapping("/kpis")
    @PreAuthorize("hasAnyRole('RESOURCE-MANAGER')")
    public ResponseEntity<DashboardKpiDTO> getDashboardKpis(
            @Parameter(description = "Start date of allocation window (format: yyyy-MM-dd)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,

            @Parameter(description = "End date of allocation window (format: yyyy-MM-dd)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to,

            @Parameter(description = "Filter by role/designation")
            @RequestParam(required = false)
            String role,

            @Parameter(description = "Filter by working location")
            @RequestParam(required = false)
            String location,

            @Parameter(description = "Filter by employment type")
            @RequestParam(required = false)
            String employmentType,

            @Parameter(description = "Filter by minimum experience (years)")
            @RequestParam(required = false)
            Integer minExperience,

            @Parameter(description = "Filter by maximum experience (years)")
            @RequestParam(required = false)
            Integer maxExperience) {

        log.info("Received KPI request with parameters: from={}, to={}, role={}, location={}, employmentType={}, minExperience={}, maxExperience={}",
                from, to, role, location, employmentType, minExperience, maxExperience);

        if (from != null && to != null && from.isAfter(to)) {
            log.warn("Invalid date range: from date {} is after to date {}", from, to);
            return ResponseEntity.badRequest().build();
        }

        if (minExperience != null && maxExperience != null && minExperience > maxExperience) {
            log.warn("Invalid experience range: minExperience {} is greater than maxExperience {}", minExperience, maxExperience);
            return ResponseEntity.badRequest().build();
        }

        try {
            DashboardKpiDTO kpis = dashboardKpiService.calculateKpis(
                    from, to, role, location, employmentType, minExperience, maxExperience
            );

            log.info("Successfully calculated KPIs: totalResources={}, fullyAvailable={}, utilization={}%",
                    kpis.getTotalResources(), kpis.getFullyAvailable(), kpis.getUtilization());

            return ResponseEntity.ok(kpis);

        } catch (AccessDeniedException e) {
            log.error("Access denied for KPI request - User may not have required role", e);
            throw ProjectExceptionHandler.badRequest("Access denied: You do not have permission to access dashboard KPIs");
        } catch (IllegalArgumentException e) {
            log.error("Invalid parameters for KPI request: {}", e.getMessage());
            throw ProjectExceptionHandler.badRequest("Invalid parameters: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error processing KPI request", e);
            throw ProjectExceptionHandler.badRequest("Failed to process KPI request: " + e.getMessage());
        }
    }
}
