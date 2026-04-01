package com.controller.bench_controllers;

import com.dto.bench_dto.UpdateSubStateRequestDTO;
import com.dto.centralised_dto.ApiResponse;
import com.dto.bench_dto.BenchKPIDTO;
import com.dto.bench_dto.BenchResourceDTO;
import com.dto.bench_dto.BenchPoolResponseDTO;
import com.dto.bench_dto.MatchResponse;
import com.dto.allocation_dto.AllocationRequestDTO;
import com.dto.allocation_dto.QuickAllocationDTO;
import com.dto.centralised_dto.UserDTO;
import com.entity_enums.allocation_enums.AllocationStatus;
import com.dto.centralised_dto.UserDTO;
import com.security.CurrentUser;
import com.service_imple.bench_service_impl.BenchService;
import com.service_interface.bench_service_interface.BenchDemandMatchingService;
import jakarta.validation.Valid;
import com.service_interface.allocation_service_interface.AllocationService;
import com.repo.demand_repo.DemandRepository;
import com.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
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
    private final DemandRepository demandRepository;

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
    public ResponseEntity<List<MatchResponse>> getMatches(
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
            
            return ResponseEntity.ok(matches);
            
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
    @PreAuthorize("hasAnyRole('RESOURCE-MANAGER', 'PROJECT-MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<?>> quickAllocate(
            @RequestParam Long resourceId,
            @RequestParam UUID demandId,
            @RequestParam(defaultValue = "100") Integer allocationPercentage,
            @CurrentUser UserDTO user) {
        
        try {
            log.info("Quick allocating resource {} to demand {} by user {} with {}% allocation",
                    resourceId, demandId, user.getName(), allocationPercentage);
            
            // Create quick allocation DTO from parameters
            QuickAllocationDTO quickAllocation = QuickAllocationDTO.builder()
                    .resourceId(resourceId)
                    .demandId(demandId)
                    .allocationPercentage(allocationPercentage)
                    .build();
            
            // Convert quick allocation to full allocation request
            AllocationRequestDTO allocationRequest = buildAllocationRequest(quickAllocation, user);
            
            // Use existing allocation service with full validation
            return allocationService.assignAllocation(allocationRequest);
            
        } catch (Exception e) {
            log.error("Error in quick allocation: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, "Allocation failed: " + e.getMessage(), null));
        }
    }

    /**
     * Build full AllocationRequestDTO from QuickAllocationDTO with smart defaults
     */
    private AllocationRequestDTO buildAllocationRequest(QuickAllocationDTO quickAllocation, UserDTO user) {
        return AllocationRequestDTO.builder()
                .resourceId(List.of(quickAllocation.getResourceId()))
                .demandId(quickAllocation.getDemandId())
                .allocationPercentage(quickAllocation.getAllocationPercentage())
                .allocationStartDate(calculateStartDate(quickAllocation.getDemandId()))
                .allocationEndDate(calculateEndDate(quickAllocation.getDemandId()))
                .allocationStatus(AllocationStatus.PLANNED)
                .createdBy(user.getName())
                .skipValidation(true)  // Skip demand status validation for quick allocation
                .build();
    }

    /**
     * Calculate smart start date based on demand
     */
    private java.time.LocalDate calculateStartDate(UUID demandId) {
        return demandRepository.findById(demandId)
                .map(demand -> {
                    java.time.LocalDate demandStart = demand.getDemandStartDate();
                    java.time.LocalDate today = java.time.LocalDate.now();
                    // Use demand start date if it's today or future, otherwise start today
                    return demandStart.isAfter(today) ? demandStart : today;
                })
                .orElseGet(java.time.LocalDate::now); // Fallback to today if demand not found
    }

    /**
     * Calculate smart end date based on demand
     */
    private java.time.LocalDate calculateEndDate(UUID demandId) {
        return demandRepository.findById(demandId)
                .map(demand -> demand.getDemandEndDate())
                .orElseGet(() -> java.time.LocalDate.now().plusMonths(6)); // Fallback to 6 months
    }
}
