package com.controller.bench_controllers;

import com.dto.centralised_dto.ApiResponse;
import com.dto.bench_dto.BenchResourceDTO;
import com.dto.bench_dto.BenchPoolResponseDTO;
import com.service_imple.bench_service_impl.BenchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
}
