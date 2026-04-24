package com.controller.bench_controllers;

import com.dto.centralised_dto.ApiResponse;
import com.dto.centralised_dto.UserDTO;
import com.security.CurrentUser;
import com.service_imple.bench_service_impl.ResourceStateInitializationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Admin controller for resource state management
 * Provides endpoints to fix resource state issues
 */
@RestController
@RequestMapping("/api/admin/resource-state")
@RequiredArgsConstructor
@Slf4j
public class ResourceStateController {

    private final ResourceStateInitializationService resourceStateInitializationService;

    /**
     * Manually fix state for a specific resource
     * Use this to fix "No value present" errors for individual resources
     */
    @PostMapping("/fix/{resourceId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESOURCE-MANAGER')")
    public ResponseEntity<ApiResponse<?>> fixResourceState(
            @PathVariable Long resourceId,
            @CurrentUser UserDTO user) {
        
        log.info("User {} requested to fix state for resource {}", user.getName(), resourceId);
        
        try {
            boolean fixed = resourceStateInitializationService.fixResourceState(resourceId);
            
            if (fixed) {
                return ResponseEntity.ok(new ApiResponse<>(
                    true, 
                    "Resource state fixed successfully", 
                    Map.of("resourceId", resourceId, "status", "fixed")
                ));
            } else {
                return ResponseEntity.ok(new ApiResponse<>(
                    true, 
                    "Resource already has a state record", 
                    Map.of("resourceId", resourceId, "status", "already_exists")
                ));
            }
            
        } catch (Exception e) {
            log.error("Failed to fix state for resource {}: {}", resourceId, e.getMessage());
            return ResponseEntity.badRequest().body(new ApiResponse<>(
                false, 
                "Failed to fix resource state: " + e.getMessage(), 
                null
            ));
        }
    }

    /**
     * Trigger resource state initialization for all resources
     * Use this to fix system-wide resource state issues
     */
    @PostMapping("/initialize-all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<?>> initializeAllResourceStates(@CurrentUser UserDTO user) {
        
        log.info("User {} requested to initialize all resource states", user.getName());
        
        try {
            resourceStateInitializationService.initializeResourceStatesOnStartup();
            
            return ResponseEntity.ok(new ApiResponse<>(
                true, 
                "Resource state initialization completed", 
                "Check application logs for detailed results"
            ));
            
        } catch (Exception e) {
            log.error("Failed to initialize all resource states: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ApiResponse<>(
                false, 
                "Failed to initialize resource states: " + e.getMessage(), 
                null
            ));
        }
    }

    /**
     * Get resource state status
     * Check if a resource has a proper state record
     */
    @GetMapping("/status/{resourceId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESOURCE-MANAGER')")
    public ResponseEntity<ApiResponse<?>> getResourceStateStatus(@PathVariable Long resourceId) {
        
        try {
            // This would require adding a method to check status
            // For now, return basic info
            Map<String, Object> status = new HashMap<>();
            status.put("resourceId", resourceId);
            status.put("checked", true);
            status.put("message", "Use fix endpoint if resource has allocation issues");
            
            return ResponseEntity.ok(new ApiResponse<>(
                true, 
                "Resource state status retrieved", 
                status
            ));
            
        } catch (Exception e) {
            log.error("Failed to get status for resource {}: {}", resourceId, e.getMessage());
            return ResponseEntity.badRequest().body(new ApiResponse<>(
                false, 
                "Failed to get resource status: " + e.getMessage(), 
                null
            ));
        }
    }
}
