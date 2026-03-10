package com.controller.availability_controllers;

import com.dto.ApiResponse;
import com.dto.ResourceTimelineDTO;
import com.dto.ResourceTimelineResponseDTO;
import com.dto.ResourceTimelineApiResponse;
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
        ApiResponse<String> response = new ApiResponse<>();
        response.setSuccess(true);
        response.setMessage("Monthly availability sync initiated successfully");
        response.setData("Monthly availability sync initiated for " + yearMonth);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/recalculate/resource/{resourceId}")
    public ResponseEntity<ApiResponse<String>> recalculateForResource(
                        @PathVariable Long resourceId,
                        @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth yearMonth) {
        
                triggerService.triggerResourceRecalculation(resourceId, yearMonth);
        ApiResponse<String> response = new ApiResponse<>();
        response.setSuccess(true);
        response.setMessage("Resource recalculation initiated successfully");
        response.setData("Recalculation initiated for resource " + resourceId + " for " + yearMonth);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/sync/bulk")
    public ResponseEntity<ApiResponse<String>> triggerBulkRecalculation(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth startMonth,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth endMonth) {

                triggerService.triggerBulkRecalculation(startMonth, endMonth);
        ApiResponse<String> response = new ApiResponse<>();
        response.setSuccess(true);
        response.setMessage("Bulk recalculation initiated successfully");
        response.setData("Bulk recalculation initiated from " + startMonth + " to " + endMonth);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/trigger/holiday-change")
    public ResponseEntity<ApiResponse<String>> handleHolidayDataChange(
            @RequestParam Integer year) {
        
                triggerService.handleHolidayDataChange(year);
        ApiResponse<String> response = new ApiResponse<>();
        response.setSuccess(true);
        response.setMessage("Holiday change handling initiated successfully");
        response.setData("Holiday change handling initiated for year " + year);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/timeline")
    @PreAuthorize("hasAnyRole('RESOURCE-MANAGER', 'PROJECT-MANAGER')")
    public ResponseEntity<ApiResponse<List<ResourceTimelineDTO>>> getAllResourceTimelines() {
                List<ResourceTimelineDTO> timelines = resourceTimelineService.getAllResourceTimelines();
                ApiResponse<List<ResourceTimelineDTO>> response = new ApiResponse<>();
                response.setSuccess(true);
                response.setMessage("Resource timelines retrieved successfully");
                response.setData(timelines);
                return ResponseEntity.ok(response);
    }

    @GetMapping("/timeline/window")
    @PreAuthorize("hasAnyRole('RESOURCE-MANAGER', 'PROJECT-MANAGER')")
    public ResponseEntity<ResourceTimelineApiResponse> getResourceTimelineWindow(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            
            @RequestParam(required = false) String designation,
            
            @RequestParam(required = false) String location,
            
            @RequestParam(required = false) Integer minExp,
            
            @RequestParam(required = false) Integer maxExp,
            
            @RequestParam(required = false) String employmentType,
            
            @RequestParam(required = false) String status,

            @RequestParam(required = false) String search,
            
            @RequestParam(defaultValue = "0") Integer page,
            
            @RequestParam(defaultValue = "20") Integer size) {
        
                
        // Validate status if provided
        if (status != null && !List.of("available", "partial", "allocated").contains(status.toLowerCase())) {
            return ResponseEntity.badRequest()
                    .body(ResourceTimelineApiResponse.error("Invalid status. Must be one of: available, partial, allocated"));
        }
        
        ResourceTimelineApiResponse result = resourceTimelineService.getResourceTimelineWindow(
                startDate, endDate, designation, location, minExp, maxExp, 
                employmentType, status != null ? status.toLowerCase() : null, search, page, size);
        
        if (!result.getSuccess()) {
            return ResponseEntity.badRequest().body(result);
        }
        
                return ResponseEntity.ok(result);
    }

    @GetMapping("/timeline/window/kpi")
    @PreAuthorize("hasAnyRole('RESOURCE-MANAGER', 'PROJECT-MANAGER')")
    public ResponseEntity<ApiResponse<ResourceTimelineResponseDTO.TimelineKPI>> getTimelineKPI(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            
            @RequestParam(required = false) String designation,
            
            @RequestParam(required = false) String location,
            
            @RequestParam(required = false) Integer minExp,
            
            @RequestParam(required = false) Integer maxExp,
            
            @RequestParam(required = false) String employmentType,
            
            @RequestParam(required = false) String status) {
        
                
        // Validate status if provided
        if (status != null && !List.of("available", "partial", "allocated").contains(status.toLowerCase())) {
            ApiResponse<ResourceTimelineResponseDTO.TimelineKPI> errorResponse = new ApiResponse<>();
            errorResponse.setSuccess(false);
            errorResponse.setMessage("Invalid status. Must be one of: available, partial, allocated");
            errorResponse.setData(null);
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        ResourceTimelineResponseDTO.TimelineKPI kpi = resourceTimelineService.getTimelineKPI(
                startDate, endDate, designation, location, minExp, maxExp, 
                employmentType, status != null ? status.toLowerCase() : null);
        
        ApiResponse<ResourceTimelineResponseDTO.TimelineKPI> response = new ApiResponse<>();
        response.setSuccess(true);
        response.setMessage("Timeline KPI retrieved successfully");
        response.setData(kpi);
                
        return ResponseEntity.ok(response);
    }
}
