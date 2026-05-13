package com.controller.bench_controllers;

import com.dto.bench_dto.UpdateSubStateRequestDTO;
import com.dto.centralised_dto.ApiResponse;
import com.dto.bench_dto.BenchKPIDTO;
import com.dto.bench_dto.BenchResourceDTO;
import com.dto.bench_dto.BenchPoolResponseDTO;
import com.dto.bench_dto.ResourceMatchResponse;
import com.dto.centralised_dto.UserDTO;
import com.entity.bench.ResourceState;
import com.security.CurrentUser;
import com.service_imple.bench_service_impl.BenchService;
import com.service_interface.bench_service_interface.BenchDemandMatchingService;
import jakarta.validation.Valid;
import com.service_interface.allocation_service_interface.AllocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Bench Controller for managing bench resources and providing frontend APIs
 */
@RestController
@RequestMapping("/api/bench")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", allowCredentials = "false")
public class BenchController {

    private final BenchService benchDetectionService;
    private final BenchDemandMatchingService benchDemandMatchingService;
    private final AllocationService allocationService;

    /**
     * Get all bench resources
     * GET /api/v1/bench/resources
     */
    @GetMapping("/resources")
    @PreAuthorize("hasAnyRole('Resource_Manager', 'Delivery_Manager', 'Admin', 'Project_Manager')")
    public ResponseEntity<ApiResponse<List<BenchResourceDTO>>> getAllBenchResources() {
        log.info("Fetching all bench resources");
        List<BenchResourceDTO> benchResources = benchDetectionService.getAllBenchResources();
        return ResponseEntity.ok(ApiResponse.success("Bench resources retrieved successfully", benchResources));
    }

    /**
     * Get bench statistics
     * GET /api/v1/bench/statistics
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('Resource_Manager', 'Delivery_Manager', 'Admin', 'Project_Manager')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBenchStatistics() {
        log.info("Fetching bench statistics");
        Map<String, Object> statistics = benchDetectionService.getBenchStatistics();
        return ResponseEntity.ok(ApiResponse.success("Bench statistics retrieved successfully", statistics));
    }

    /**
     * Get bench resource count
     * GET /api/v1/bench/count
     */
    @GetMapping("/count")
    @PreAuthorize("hasAnyRole('Resource_Manager', 'Delivery_Manager', 'Admin', 'Project_Manager')")
    public ResponseEntity<ApiResponse<Long>> getBenchResourceCount() {
        log.info("Fetching bench resource count");
        long count = benchDetectionService.getBenchResourceCount();
        return ResponseEntity.ok(ApiResponse.success("Bench resource count retrieved successfully", count));
    }

    /**
     * Trigger bench detection manually (for admin/testing purposes)
     * POST /api/v1/bench/detect
     */
    @PostMapping("/detect")
    @PreAuthorize("hasAnyRole('Resource_Manager', 'Admin')")
    public ResponseEntity<ApiResponse<String>> triggerBenchDetection() {
        log.info("Manual bench detection triggered");
        benchDetectionService.detectBenchResources();
        return ResponseEntity.ok(ApiResponse.success("Bench detection completed successfully", "Detection process completed"));
    }

    /**
     * Get bench resources for bench endpoint
     * GET /api/bench/bench-resources
     */
    @GetMapping("/bench-resources")
    @PreAuthorize("hasAnyRole('Resource_Manager', 'Delivery_Manager', 'Admin', 'Project_Manager')")
    public ResponseEntity<ApiResponse<List<BenchPoolResponseDTO>>> getBenchResources() {
        List<BenchPoolResponseDTO> benchResources = benchDetectionService.getBenchResources();
        return ResponseEntity.ok(ApiResponse.success("Bench resources retrieved successfully", benchResources));
    }

    /**
     * Get pool resources for pool endpoint
     * GET /api/bench/pool-resources
     */
    @GetMapping("/pool-resources")
    @PreAuthorize("hasAnyRole('Resource_Manager', 'Delivery_Manager', 'Admin', 'Project_Manager')")
    public ResponseEntity<ApiResponse<List<BenchPoolResponseDTO>>> getPoolResources() {
        List<BenchPoolResponseDTO> poolResources = benchDetectionService.getPoolResources();
        return ResponseEntity.ok(ApiResponse.success("Pool resources retrieved successfully", poolResources));
    }

    @GetMapping("/high-risk")
    @PreAuthorize("hasAnyRole('Resource_Manager', 'Delivery_Manager', 'Admin')")
    public ResponseEntity<ApiResponse<List<BenchResourceDTO>>> getHighRiskBench() {
        List<BenchResourceDTO> highRiskResources = benchDetectionService.getHighRiskBenchResources();
        return ResponseEntity.ok(ApiResponse.success("High risk resources retrieved successfully", highRiskResources));
    }

    /**
     * Get bench KPI metrics
     * GET /api/bench/kpi
     */
    @GetMapping("/kpi")
    @PreAuthorize("hasAnyRole('Resource_Manager', 'Delivery_Manager', 'Admin', 'Project_Manager')")
    public ResponseEntity<ApiResponse<BenchKPIDTO>> getBenchKPI() {
        BenchKPIDTO kpi = benchDetectionService.getBenchKPI();
        return ResponseEntity.ok(ApiResponse.success("Bench KPI metrics retrieved successfully", kpi));
    }

    @PutMapping("/update-resource-state")
    @PreAuthorize("hasRole('Resource_Manager')")
    public ResponseEntity<ApiResponse<ResourceState>> updateResourceState(@Valid @RequestBody UpdateSubStateRequestDTO request, @CurrentUser UserDTO userDTO) {
        return benchDetectionService.updateSubState(request, userDTO);
    }

    /**
     * Get bench-to-demand matches (high quality matches >70%)
     * GET /api/bench/matches
     */
    @GetMapping("/matches")
    @PreAuthorize("hasAnyRole('Resource_Manager', 'Delivery_Manager', 'Admin', 'Project_Manager')")
    public ResponseEntity<ApiResponse<List<ResourceMatchResponse>>> getMatches(
            @RequestParam(required = false) String skill,
            @RequestParam(required = false) Integer minExp) {

        log.info("Getting bench-demand matches with filters - skill: {}, minExp: {} (APPROVED demands only)", skill, minExp);
        List<ResourceMatchResponse> matches = benchDemandMatchingService.getHighQualityResourceMatches(skill, minExp);
        return ResponseEntity.ok(ApiResponse.success("High quality matches retrieved successfully", matches));
    }

    /**
     * Quick allocate bench resource to demand
     * POST /api/bench/quick-allocate
     */
    @PostMapping("/quick-allocate")
    @PreAuthorize("hasAnyRole('Resource_Manager', 'Project_Manager', 'Admin')")
    public ResponseEntity<ApiResponse<?>> quickAllocate(
            @RequestParam String resourceId,
            @RequestParam UUID demandId,
            @RequestParam(defaultValue = "100") Integer allocationPercentage,
            @CurrentUser UserDTO user) {

        log.info("Quick allocating resource {} to demand {} by user {} with {}% allocation",
                resourceId, demandId, user.getName(), allocationPercentage);
        return allocationService.quickAllocateResource(resourceId, demandId, allocationPercentage, user);
    }
}
