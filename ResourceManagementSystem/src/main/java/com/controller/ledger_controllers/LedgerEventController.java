package com.controller.ledger_controllers;

import com.dto.ledger_dto.LedgerAllocationChangedRequest;
import com.dto.ledger_dto.LedgerResourceCreatedRequest;
import com.dto.ledger_dto.LedgerRoleOffEventRequest;
import com.events.ledger_events.AllocationChangedEvent;
import com.events.ledger_events.ResourceCreatedEvent;
import com.events.ledger_events.RoleOffEvent;
import com.events.publisher.EventPublishingService;
import com.service_interface.ledger_service_interface.AvailabilityCalculationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/ledger/events")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Ledger Event Management", description = "APIs for triggering and managing ledger events")
public class LedgerEventController {

    private final EventPublishingService eventPublishingService;
    private final AvailabilityCalculationService availabilityCalculationService;

    @PostMapping("/allocation-changed")
    @Operation(summary = "Trigger allocation changed event", description = "Publishes an allocation changed event to trigger ledger recalculation")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> triggerAllocationChangedEvent(
            @RequestBody LedgerAllocationChangedRequest request) {
        
        try {
            AllocationChangedEvent event = AllocationChangedEvent.builder()
                    .eventId(request.getEventId())
                    .allocationId(request.getAllocationId())
                    .resourceId(request.getResourceId())
                    .projectId(request.getProjectId())
                    .demandId(request.getDemandId())
                    .changeType(request.getChangeType())
                    .effectiveStartDate(request.getEffectiveStartDate())
                    .effectiveEndDate(request.getEffectiveEndDate())
                    .previousStartDate(request.getPreviousStartDate())
                    .previousEndDate(request.getPreviousEndDate())
                    .previousAllocationPercentage(request.getPreviousAllocationPercentage())
                    .newAllocationPercentage(request.getNewAllocationPercentage())
                    .allocationStatus(request.getAllocationStatus())
                    .previousAllocationStatus(request.getPreviousAllocationStatus())
                    .eventTimestamp(LocalDateTime.now())
                    .changedBy(request.getChangedBy())
                    .changeReason(request.getChangeReason())
                    .eventType("ALLOCATION_CHANGED")
                    .eventSource("ALLOCATION_SERVICE")
                    .build();

            return eventPublishingService.publishAllocationChangedAsync(event)
                    .thenApply(v -> ResponseEntity.ok(Map.<String, Object>of(
                            "status", "success",
                            "eventId", event.getEventId(),
                            "message", "Allocation changed event published successfully"
                    )))
                    .exceptionally(e -> {
                        log.error("Failed to publish allocation changed event: {}", e.getMessage());
                        return ResponseEntity.internalServerError().body(Map.of(
                                "status", "error",
                                "message", "Failed to publish allocation changed event: " + e.getMessage()
                        ));
                    });

        } catch (Exception e) {
            log.error("Error processing allocation changed event request: {}", e.getMessage(), e);
            return CompletableFuture.completedFuture(ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Invalid request: " + e.getMessage())
            ));
        }
    }

    @PostMapping("/role-off")
    @Operation(summary = "Trigger role-off event", description = "Publishes a role-off event to trigger ledger recalculation")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> triggerRoleOffEvent(
            @RequestBody LedgerRoleOffEventRequest request) {
        
        try {
            RoleOffEvent event = RoleOffEvent.builder()
                    .eventId(request.getEventId())
                    .allocationId(request.getAllocationId())
                    .resourceId(request.getResourceId())
                    .projectId(request.getProjectId())
                    .demandId(request.getDemandId())
                    .roleOffDate(request.getRoleOffDate())
                    .allocationEndDate(request.getAllocationEndDate())
                    .roleOffReason(request.getRoleOffReason())
                    .roleOffStatus(request.getRoleOffStatus())
                    .eventTimestamp(LocalDateTime.now())
                    .roleOffInitiatedBy(request.getRoleOffInitiatedBy())
                    .roleOffComments(request.getRoleOffComments())
                    .isProjectClosure(request.getIsProjectClosure())
                    .projectEndDate(request.getProjectEndDate())
                    .eventType("ROLE_OFF")
                    .eventSource("ROLE_OFF_SERVICE")
                    .build();

            return eventPublishingService.publishRoleOffAsync(event)
                    .thenApply(v -> ResponseEntity.ok(Map.<String, Object>of(
                            "status", "success",
                            "eventId", event.getEventId(),
                            "message", "Role-off event published successfully"
                    )))
                    .exceptionally(e -> {
                        log.error("Failed to publish role-off event: {}", e.getMessage());
                        return ResponseEntity.internalServerError().body(Map.of(
                                "status", "error",
                                "message", "Failed to publish role-off event: " + e.getMessage())
                        );
                    });

        } catch (Exception e) {
            log.error("Error processing role-off event request: {}", e.getMessage(), e);
            return CompletableFuture.completedFuture(ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Invalid request: " + e.getMessage())
            ));
        }
    }

    @PostMapping("/resource-created")
    @Operation(summary = "Trigger resource created event", description = "Publishes a resource created event to trigger ledger calculation")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> triggerResourceCreatedEvent(
            @RequestBody LedgerResourceCreatedRequest request) {
        
        try {
            ResourceCreatedEvent event = ResourceCreatedEvent.builder()
                    .eventId(request.getEventId())
                    .resourceId(request.getResourceId())
                    .employeeId(request.getEmployeeId())
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .email(request.getEmail())
                    .dateOfJoining(request.getDateOfJoining())
                    .dateOfExit(request.getDateOfExit())
                    .employmentStatus(request.getEmploymentStatus())
                    .employmentType(request.getEmploymentType())
                    .primarySkills(request.getPrimarySkills())
                    .location(request.getLocation())
                    .department(request.getDepartment())
                    .designation(request.getDesignation())
                    .eventTimestamp(LocalDateTime.now())
                    .createdBy(request.getCreatedBy())
                    .eventType("RESOURCE_CREATED")
                    .eventSource("RESOURCE_SERVICE")
                    .build();

            return eventPublishingService.publishResourceCreatedAsync(event)
                    .thenApply(v -> ResponseEntity.ok(Map.<String, Object>of(
                            "status", "success",
                            "eventId", event.getEventId(),
                            "message", "Resource created event published successfully"
                    )))
                    .exceptionally(e -> {
                        log.error("Failed to publish resource created event: {}", e.getMessage());
                        return ResponseEntity.internalServerError().body(Map.of(
                                "status", "error",
                                "message", "Failed to publish resource created event: " + e.getMessage())
                        );
                    });

        } catch (Exception e) {
            log.error("Error processing resource created event request: {}", e.getMessage(), e);
            return CompletableFuture.completedFuture(ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Invalid request: " + e.getMessage())
            ));
        }
    }

    @PostMapping("/recalculate")
    @Operation(summary = "Manual recalculation", description = "Manually trigger availability recalculation for a resource")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> triggerManualRecalculation(
            @RequestParam @Parameter(description = "Resource ID") Long resourceId,
            @RequestParam @Parameter(description = "Start date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @Parameter(description = "End date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        return CompletableFuture.runAsync(() -> {
            try {
                availabilityCalculationService.recalculateForDateRange(resourceId, startDate, endDate);
            } catch (Exception e) {
                log.error("Failed to recalculate availability for resource {}: {}", resourceId, e.getMessage(), e);
                throw new RuntimeException("Recalculation failed", e);
            }
        }).thenApply(v -> {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("resourceId", resourceId);
            response.put("startDate", startDate);
            response.put("endDate", endDate);
            response.put("message", "Recalculation completed successfully");
            return ResponseEntity.ok(response);
        }).exceptionally(e -> {
            log.error("Manual recalculation failed for resource {}: {}", resourceId, e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Recalculation failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        });
    }

    @PostMapping("/incremental-update")
    @Operation(summary = "Trigger incremental update", description = "Trigger incremental availability update based on event date")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> triggerIncrementalUpdate(
            @RequestParam @Parameter(description = "Resource ID") Long resourceId,
            @RequestParam @Parameter(description = "Event date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate eventDate,
            @RequestParam @Parameter(description = "Event type") String eventType) {
        
        try {
            LocalDate startDate = eventDate.isBefore(LocalDate.now().minusDays(30)) ? LocalDate.now().minusDays(30) : eventDate;
            LocalDate endDate = eventDate.plusDays(90);
            
            availabilityCalculationService.recalculateForDateRange(resourceId, startDate, endDate);
            
            return CompletableFuture.completedFuture(ResponseEntity.ok(Map.<String, Object>of(
                    "status", "success",
                    "resourceId", resourceId,
                    "eventDate", eventDate,
                    "eventType", eventType,
                    "message", "Incremental update triggered successfully"
            )));
            
        } catch (Exception e) {
            log.error("Failed to trigger incremental update for resource {}: {}", resourceId, e.getMessage(), e);
            return CompletableFuture.completedFuture(ResponseEntity.internalServerError().body(Map.<String, Object>of(
                    "status", "error",
                    "message", "Incremental update failed: " + e.getMessage())
            ));
        }
    }

    @GetMapping("/availability/{resourceId}")
    @Operation(summary = "Get availability data", description = "Get availability data for a resource within date range")
    public ResponseEntity<Map<String, Object>> getAvailabilityData(
            @PathVariable @Parameter(description = "Resource ID") Long resourceId,
            @RequestParam @Parameter(description = "Start date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @Parameter(description = "End date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        try {
            Map<String, Object> availabilityData = availabilityCalculationService.getAvailabilitySummary(resourceId, startDate, endDate);
            return ResponseEntity.ok(availabilityData);
        } catch (Exception e) {
            log.error("Failed to get availability data for resource {}: {}", resourceId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", "Failed to get availability data: " + e.getMessage())
            );
        }
    }
}
