package com.controller.availability_controllers;

import com.dto.centralised_dto.ApiResponse;
import com.dto.availability_dto.ResourceTimelineDTO;
import com.dto.availability_dto.ResourceTimelineResponseDTO;
import com.service_interface.availability_service_interface.AvailabilityTriggerService;
import com.service_interface.availability_service_interface.ResourceTimelineService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/availability")
@RequiredArgsConstructor
public class AvailabilityController {

    private final AvailabilityTriggerService triggerService;
    private final ResourceTimelineService resourceTimelineService;

    @PostMapping("/sync/monthly")
    public ResponseEntity<ApiResponse<String>> triggerMonthlySync(
                        @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth yearMonth) {
        
                triggerService.triggerMonthlySync(yearMonth);
        return ResponseEntity.ok(ApiResponse.success("Monthly availability sync initiated successfully", "Monthly availability sync initiated for " + yearMonth));
    }

    @PostMapping("/recalculate/resource/{resourceId}")
    public ResponseEntity<ApiResponse<String>> recalculateForResource(
                        @PathVariable String resourceId,
                        @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth yearMonth) {
        
                triggerService.triggerResourceRecalculation(resourceId, yearMonth);
        return ResponseEntity.ok(ApiResponse.success("Resource recalculation initiated successfully", "Recalculation initiated for resource " + resourceId + " for " + yearMonth));
    }
    
    @PostMapping("/sync/bulk")
    public ResponseEntity<ApiResponse<String>> triggerBulkRecalculation(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth startMonth,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth endMonth) {

                triggerService.triggerBulkRecalculation(startMonth, endMonth);
        return ResponseEntity.ok(ApiResponse.success("Bulk recalculation initiated successfully", "Bulk recalculation initiated from " + startMonth + " to " + endMonth));
    }

    @PostMapping("/trigger/holiday-change")
    public ResponseEntity<ApiResponse<String>> handleHolidayDataChange(
            @RequestParam Integer year) {
        
                triggerService.handleHolidayDataChange(year);
        return ResponseEntity.ok(ApiResponse.success("Holiday change handling initiated successfully", "Holiday change handling initiated for year " + year));
    }

    @GetMapping("/timeline")
    @PreAuthorize("hasAnyRole('Resource_Manager', 'Project_Manager')")
    public ResponseEntity<ApiResponse<List<ResourceTimelineDTO>>> getAllResourceTimelines() {
                List<ResourceTimelineDTO> timelines = resourceTimelineService.getAllResourceTimelines();
                return ResponseEntity.ok(ApiResponse.success("Resource timelines retrieved successfully", timelines));
    }

    @GetMapping("/timeline/window")
    @PreAuthorize("hasAnyRole('Resource_Manager', 'Project_Manager')")
    public ResponseEntity<ApiResponse<?>> getResourceTimelineWindow(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            
            @RequestParam(required = false) String designation,
            
            @RequestParam(required = false) String location,
            
            @RequestParam(required = false) Integer minExp,
            
            @RequestParam(required = false) Integer maxExp,
            
            @RequestParam(required = false) String employmentType,
            
            @RequestParam(required = false) String status,

            @RequestParam(required = false) String search,
            
            @RequestParam(required = false) Integer allocationPercentage,
            
            @RequestParam(required = false) String project,
            
            @RequestParam(defaultValue = "0") Integer page,
            
            @RequestParam(defaultValue = "20") Integer size) {
        if (status != null && !List.of("available", "partial", "allocated").contains(status.toLowerCase())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid status. Must be one of: available, partial, allocated"));
        }
        
        ApiResponse<?> result = resourceTimelineService.getResourceTimelineWindow(
                startDate, endDate, designation, location, minExp, maxExp, 
                employmentType, status != null ? status.toLowerCase() : null, search, allocationPercentage, project, page, size);
        
        if (Boolean.FALSE.equals(result.getSuccess())) {
            return ResponseEntity.badRequest().body(result);
        }
        
        return ResponseEntity.ok(result);
    }

    @GetMapping("/timeline/window/kpi")
    @PreAuthorize("hasAnyRole('Resource_Manager', 'Project_Manager')")
    public ResponseEntity<ApiResponse<ResourceTimelineResponseDTO.ResourceTimelineKpi>> getTimelineKPI(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            
            @RequestParam(required = false) String designation,
            
            @RequestParam(required = false) String location,
            
            @RequestParam(required = false) Integer minExp,
            
            @RequestParam(required = false) Integer maxExp,
            
            @RequestParam(required = false) String employmentType,
            
            @RequestParam(required = false) String status) {
        if (status != null && !List.of("available", "partial", "allocated").contains(status.toLowerCase())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid status. Must be one of: available, partial, allocated"));
        }
        
        ResourceTimelineResponseDTO.ResourceTimelineKpi kpi = resourceTimelineService.getTimelineKPI(
                startDate, endDate, designation, location, minExp, maxExp, 
                employmentType, status != null ? status.toLowerCase() : null);
        
        return ResponseEntity.ok(ApiResponse.success("Data fetched successfully", kpi));
    }
}
