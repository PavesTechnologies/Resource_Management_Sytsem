package com.controller.bench_controllers;

import com.dto.bench_dto.UpdateSubStateRequestDTO;
import com.dto.centralised_dto.ApiResponse;
import com.dto.bench_dto.BenchKPIDTO;
import com.dto.bench_dto.BenchResourceDTO;
import com.dto.bench_dto.BenchPoolResponseDTO;
import com.dto.bench_dto.MatchResponse;
import com.dto.bench_dto.ResourceMatchResponse;
import com.dto.bench_dto.DemandMatch;
import com.dto.centralised_dto.UserDTO;
import com.security.CurrentUser;
import com.service_imple.bench_service_impl.BenchService;
import com.service_interface.bench_service_interface.BenchDemandMatchingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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

    /**
     * Get all bench resources
     * GET /api/v1/bench/resources
     */
    @GetMapping("/resources")
    public ResponseEntity<ApiResponse<List<BenchResourceDTO>>> getAllBenchResources() {
        try {
            log.info("Fetching all bench resources");
            
            List<BenchResourceDTO> benchResources = benchDetectionService.getAllBenchResources();
            
            return ResponseEntity.ok(
                new ApiResponse<>(true, "Bench resources retrieved successfully", benchResources)
            );
            
        } catch (Exception e) {
            log.error("Error fetching bench resources: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "Error fetching bench resources: " + e.getMessage(), null));
        }
    }

    /**
     * Get bench statistics
     * GET /api/v1/bench/statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBenchStatistics() {
        try {
            log.info("Fetching bench statistics");
            
            Map<String, Object> statistics = benchDetectionService.getBenchStatistics();
            
            return ResponseEntity.ok(
                new ApiResponse<>(true, "Bench statistics retrieved successfully", statistics)
            );
            
        } catch (Exception e) {
            log.error("Error fetching bench statistics: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "Error fetching bench statistics: " + e.getMessage(), null));
        }
    }

    /**
     * Get bench resource count
     * GET /api/v1/bench/count
     */
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> getBenchResourceCount() {
        try {
            log.info("Fetching bench resource count");
            
            long count = benchDetectionService.getBenchResourceCount();
            
            return ResponseEntity.ok(
                new ApiResponse<>(true, "Bench resource count retrieved successfully", count)
            );
            
        } catch (Exception e) {
            log.error("Error fetching bench resource count: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "Error fetching bench resource count: " + e.getMessage(), null));
        }
    }

    /**
     * Trigger bench detection manually (for admin/testing purposes)
     * POST /api/v1/bench/detect
     */
    @PostMapping("/detect")
    public ResponseEntity<ApiResponse<String>> triggerBenchDetection() {
        try {
            log.info("Manual bench detection triggered");
            
            benchDetectionService.detectBenchResources();
            
            return ResponseEntity.ok(
                new ApiResponse<>(true, "Bench detection completed successfully", "Detection process completed")
            );
            
        } catch (Exception e) {
            log.error("Error during manual bench detection: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "Error during bench detection: " + e.getMessage(), null));
        }
    }

    /**
     * Get bench resources for bench endpoint
     * GET /api/bench/bench-resources
     */
    @GetMapping("/bench-resources")
    public ResponseEntity<ApiResponse<List<BenchPoolResponseDTO>>> getBenchResources() {
        List<BenchPoolResponseDTO> benchResources = benchDetectionService.getBenchResources();
        return ResponseEntity.ok(
            new ApiResponse<>(true, "Bench resources retrieved successfully", benchResources)
        );
    }

    /**
     * Get pool resources for pool endpoint
     * GET /api/bench/pool-resources
     */
    @GetMapping("/pool-resources")
    public ResponseEntity<ApiResponse<List<BenchPoolResponseDTO>>> getPoolResources() {
        List<BenchPoolResponseDTO> poolResources = benchDetectionService.getPoolResources();
        return ResponseEntity.ok(
            new ApiResponse<>(true, "Pool resources retrieved successfully", poolResources)
        );
    }
    @GetMapping("/high-risk")
    public ResponseEntity<ApiResponse<List<BenchResourceDTO>>> getHighRiskBench() {

        List<BenchResourceDTO> list = benchDetectionService.getAllBenchResources()
                .stream()
                .filter(dto -> "HIGH".equals(dto.getRiskLevel())
                        || "CRITICAL".equals(dto.getRiskLevel()))
                .toList();

        return ResponseEntity.ok(
                new ApiResponse<>(true, "High risk resources", list)
        );
    }

    /**
     * Get bench KPI metrics
     * GET /api/bench/kpi
     */
    @GetMapping("/kpi")
    public ResponseEntity<ApiResponse<BenchKPIDTO>> getBenchKPI() {
        BenchKPIDTO kpi = benchDetectionService.getBenchKPI();
        return ResponseEntity.ok(
                new ApiResponse<>(true, "Bench KPI metrics retrieved successfully", kpi)
        );
    }

    @PutMapping("/update-resource-state")
    @PreAuthorize("hasRole('RESOURCE-MANAGER')")
    public ResponseEntity<?> updateResourceState(@Valid @RequestBody UpdateSubStateRequestDTO request, @CurrentUser UserDTO userDTO) {
        return benchDetectionService.updateSubState(request, userDTO);
    }

    /**
     * Get bench-to-demand matches
     * GET /api/bench/matches
     */
    @GetMapping("/matches")
    public ResponseEntity<ApiResponse<List<ResourceMatchResponse>>> getMatches(
            @RequestParam(required = false) String skill,
            @RequestParam(required = false) Integer minExp) {

        try {
            log.info("Getting bench-demand matches with filters - skill: {}, minExp: {}", skill, minExp);

            List<MatchResponse> matches;

            if (skill != null || minExp != null) {
                matches = benchDemandMatchingService.getMatches(skill, minExp);
            } else {
                matches = benchDemandMatchingService.getMatches();
            }

            // Group matches by resource
            Map<Long, List<MatchResponse>> groupedByResource = matches.stream()
                    .collect(java.util.stream.Collectors.groupingBy(MatchResponse::getResourceId));

            List<ResourceMatchResponse> response = groupedByResource.entrySet().stream()
                    .map(entry -> {
                        Long resourceId = entry.getKey();
                        List<MatchResponse> resourceMatches = entry.getValue();
                        
                        MatchResponse firstMatch = resourceMatches.get(0);
                        
                        List<DemandMatch> demands = resourceMatches.stream()
                                .map(match -> DemandMatch.builder()
                                        .demandId(match.getDemandId())
                                        .demandName(match.getDemandName())
                                        .matchedSkills(match.getMatchedSkills())
                                        .matchScore(match.getMatchScore())
                                        .build())
                                .collect(java.util.stream.Collectors.toList());

                        return ResourceMatchResponse.builder()
                                .resourceId(firstMatch.getResourceId())
                                .resourceName(firstMatch.getResourceName())
                                .resourceExperience(firstMatch.getResourceExperience())
                                .availability(firstMatch.getAvailability())
                                .demands(demands)
                                .build();
                    })
                    .collect(java.util.stream.Collectors.toList());

            return ResponseEntity.ok().body(new ApiResponse<>(true, "Matched Demands Retrived Successfully!", response));

        } catch (Exception e) {
            log.error("Error getting bench-demand matches: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Quick allocate bench resource to demand
     * POST /api/bench/quick-allocate
     */
    @PostMapping("/quick-allocate")
    public ResponseEntity<String> quickAllocate(
            @RequestParam Long resourceId,
            @RequestParam UUID demandId) {

        try {
            log.info("Quick allocating resource {} to demand {}", resourceId, demandId);

            // TODO: Implement allocation logic
            // allocationService.createAllocation(resourceId, demandId);

            return ResponseEntity.ok("Allocation successful - TODO: Implement allocation logic");

        } catch (Exception e) {
            log.error("Error in quick allocation: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Allocation failed: " + e.getMessage());
        }
    }
}
