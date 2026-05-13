package com.controller.availability_controllers;

import com.dto.centralised_dto.ApiResponse;
import com.dto.availability_dto.DashboardKpiDTO;
import com.service_interface.availability_service_interface.DashboardKpiService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.global_exception_handler.AvailabilityExceptionHandler;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/rms")
@RequiredArgsConstructor
public class DashboardKpiController {

    private final DashboardKpiService dashboardKpiService;

    @GetMapping("/kpis")
    @PreAuthorize("hasAnyRole('Resource_Manager')")
    public ResponseEntity<ApiResponse<DashboardKpiDTO>> getDashboardKpis(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to,

            @RequestParam(required = false)
            String role,

            @RequestParam(required = false)
            String location,

            @RequestParam(required = false)
            String employmentType,

            @RequestParam(required = false)
            Integer minExperience,

            @RequestParam(required = false)
            Integer maxExperience) {

        
        if (from != null && to != null && from.isAfter(to)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid date range: 'from' date cannot be after 'to' date"));
        }

        if (minExperience != null && maxExperience != null && minExperience > maxExperience) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid experience range: minimum experience cannot be greater than maximum experience"));
        }

        try {
            DashboardKpiDTO kpis = dashboardKpiService.calculateKpis(
                    from, to, role, location, employmentType, minExperience, maxExperience
            );

            return ResponseEntity.ok(ApiResponse.success("Dashboard KPIs retrieved successfully", kpis));

        } catch (AccessDeniedException e) {
                        throw AvailabilityExceptionHandler.badRequest("Access denied: You do not have permission to access dashboard KPIs");
        } catch (IllegalArgumentException e) {
                        throw AvailabilityExceptionHandler.badRequest("Invalid parameters: " + e.getMessage());
        } catch (Exception e) {
                        throw AvailabilityExceptionHandler.badRequest("Failed to process KPI request: " + e.getMessage());
        }
    }
}
