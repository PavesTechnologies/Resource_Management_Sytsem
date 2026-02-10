package com.controller.availability_controllers;

import com.entity.availability_entities.ResourceAvailabilityLedger;
import com.service_interface.availability_interface.AvailabilityTriggerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;

@RestController
@RequestMapping("/api/availability")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Availability Management", description = "APIs for workforce availability calculation and management")
public class AvailabilityController {

    private final AvailabilityTriggerService triggerService;

    @PostMapping("/sync/monthly")
    @Operation(summary = "Trigger monthly availability sync", description = "Calculates availability for all resources for a specific month")
    public ResponseEntity<String> triggerMonthlySync(
            @Parameter(description = "Month in YYYY-MM format", example = "2026-02")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth yearMonth) {
        
        log.info("API request: Monthly sync for {}", yearMonth);
        triggerService.triggerMonthlySync(yearMonth);
        return ResponseEntity.ok("Monthly availability sync initiated for " + yearMonth);
    }

    @PostMapping("/recalculate/resource/{resourceId}")
    @Operation(summary = "Recalculate availability for specific resource", description = "Recalculates availability for a single resource for a specific month")
    public ResponseEntity<String> recalculateForResource(
            @Parameter(description = "Resource ID", example = "123")
            @PathVariable Long resourceId,
            @Parameter(description = "Month in YYYY-MM format", example = "2026-02")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth yearMonth) {
        
        log.info("API request: Recalculate for resource {} month {}", resourceId, yearMonth);
        triggerService.triggerResourceRecalculation(resourceId, yearMonth);
        return ResponseEntity.ok("Recalculation initiated for resource " + resourceId + " for " + yearMonth);
    }
    public ResponseEntity<String> triggerBulkRecalculation(
            @Parameter(description = "Start month in YYYY-MM format", example = "2026-01")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth startMonth,
            @Parameter(description = "End month in YYYY-MM format", example = "2026-12")
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth endMonth) {

        log.info("API request: Bulk recalculation from {} to {}", startMonth, endMonth);
        triggerService.triggerBulkRecalculation(startMonth, endMonth);
        return ResponseEntity.ok("Bulk recalculation initiated from " + startMonth + " to " + endMonth);
    }

    @PostMapping("/trigger/holiday-change")
    @Operation(summary = "Handle holiday data changes", description = "Triggers recalculation when holiday data changes")
    public ResponseEntity<String> handleHolidayDataChange(
            @Parameter(description = "Year", example = "2026")
            @RequestParam Integer year) {
        
        log.info("API request: Holiday data change for year {}", year);
        triggerService.handleHolidayDataChange(year);
        return ResponseEntity.ok("Holiday change handling initiated for year " + year);
    }

}
