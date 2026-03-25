package com.controller.allocation_controllers;

import com.dto.centralised_dto.ApiResponse;
import com.dto.allocation_dto.SkillGapAnalysisRequestDTO;
import com.service_interface.allocation_service_interface.AllocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Controller for Enterprise Skill Gap Matching Engine
 * Provides skill gap analysis endpoints integrated with allocation module
 */
@Slf4j
@RestController
@RequestMapping("/api/matching")
@RequiredArgsConstructor
@CrossOrigin
public class SkillGapMatchingController {

    private final AllocationService allocationService;
    private final MeterRegistry meterRegistry;
    private final AtomicLong requestCounter = new AtomicLong(0);

    /**
     * Performs comprehensive skill gap analysis between demand and resource
     *
     * @param request Contains demandId and resourceId for analysis
     * @return Comprehensive skill gap analysis with match scores, risk assessment, and detailed comparisons
     */
    @PostMapping("/skill-gap-analysis")
    @PreAuthorize("hasAnyRole('RESOURCE-MANAGER','PROJECT-MANAGER','ADMIN')")
    public ResponseEntity<?> analyzeSkillGap(
            @Valid @RequestBody SkillGapAnalysisRequestDTO request) {

        long requestId = requestCounter.incrementAndGet();
        Instant startTime = Instant.now();

        log.info("[SKILL_GAP_ANALYSIS-{}] Starting analysis for demandId: {}, resourceId: {}",
                requestId, request.getDemandId(), request.getResourceId());

        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            ResponseEntity<?> response = allocationService.analyzeSkillGap(request);

            Duration executionTime = Duration.between(startTime, Instant.now());
            sample.stop(meterRegistry.timer("skill_gap_analysis_duration_ms"));

            if (executionTime.toMillis() > 200) {
                log.warn("[SKILL_GAP_ANALYSIS-{}] SLOW QUERY: Execution time: {}ms",
                        requestId, executionTime.toMillis());
            }

            log.info("[SKILL_GAP_ANALYSIS-{}] Completed in {}ms with status: {}",
                    requestId, executionTime.toMillis(),
                    response.getBody() != null && ((ApiResponse<?>) response.getBody()).getSuccess() ? "SUCCESS" : "FAILURE");

            return response;
        } catch (Exception e) {
            Duration executionTime = Duration.between(startTime, Instant.now());
            sample.stop(meterRegistry.timer("skill_gap_analysis_duration_ms", "status", "error"));

            log.error("[SKILL_GAP_ANALYSIS-{}] Failed after {}ms: {}",
                    requestId, executionTime.toMillis(), e.getMessage(), e);
            throw e;
        }
    }
}
