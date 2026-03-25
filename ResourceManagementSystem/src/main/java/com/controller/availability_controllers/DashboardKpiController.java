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
import com.global_exception_handler.ProjectExceptionHandler;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/rms")
@RequiredArgsConstructor
public class DashboardKpiController {

    private final DashboardKpiService dashboardKpiService;

    @GetMapping("/kpis")
    @PreAuthorize("hasAnyRole('RESOURCE-MANAGER')")
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
                        ApiResponse<DashboardKpiDTO> errorResponse = new ApiResponse<>();
                        errorResponse.setSuccess(false);
                        errorResponse.setMessage("Invalid date range: 'from' date cannot be after 'to' date");
                        errorResponse.setData(null);
                        return ResponseEntity.badRequest().body(errorResponse);
        }

        if (minExperience != null && maxExperience != null && minExperience > maxExperience) {
                        ApiResponse<DashboardKpiDTO> errorResponse = new ApiResponse<>();
                        errorResponse.setSuccess(false);
                        errorResponse.setMessage("Invalid experience range: minimum experience cannot be greater than maximum experience");
                        errorResponse.setData(null);
                        return ResponseEntity.badRequest().body(errorResponse);
        }

        try {
            DashboardKpiDTO kpis = dashboardKpiService.calculateKpis(
                    from, to, role, location, employmentType, minExperience, maxExperience
            );

            ApiResponse<DashboardKpiDTO> response = new ApiResponse<>();
            response.setSuccess(true);
            response.setMessage("Dashboard KPIs retrieved successfully");
            response.setData(kpis);
            return ResponseEntity.ok(response);

        } catch (AccessDeniedException e) {
                        throw ProjectExceptionHandler.badRequest("Access denied: You do not have permission to access dashboard KPIs");
        } catch (IllegalArgumentException e) {
                        throw ProjectExceptionHandler.badRequest("Invalid parameters: " + e.getMessage());
        } catch (Exception e) {
                        throw ProjectExceptionHandler.badRequest("Failed to process KPI request: " + e.getMessage());
        }
    }
}
