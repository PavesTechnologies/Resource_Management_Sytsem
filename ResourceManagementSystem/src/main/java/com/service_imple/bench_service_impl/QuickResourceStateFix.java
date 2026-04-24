package com.service_imple.bench_service_impl;

import com.entity.bench.ResourceState;
import com.entity_enums.bench.StateType;
import com.entity_enums.bench.SubState;
import com.repo.bench_repo.BenchDetectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Quick fix utility for resource state issues
 * Use this to immediately fix the "No value present" error
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuickResourceStateFix {

    private final BenchDetectionRepository benchDetectionRepository;

    /**
     * Quick fix for resource 47 - creates a BENCH state if missing
     * This can be called manually or via admin endpoint
     */
    @Transactional
    public boolean fixResource47() {
        return fixResource(47L);
    }

    /**
     * Generic fix for any resource without state
     */
    @Transactional
    public boolean fixResource(Long resourceId) {
        log.info("Attempting to fix state for resource {}", resourceId);
        
        try {
            // Check if resource already has a state
            if (benchDetectionRepository.findCurrentState(resourceId).isPresent()) {
                log.info("Resource {} already has a state record", resourceId);
                return false;
            }
            
            // Create a new BENCH state
            ResourceState newState = ResourceState.builder()
                    .resourceId(resourceId)
                    .stateType(StateType.BENCH)
                    .subState(SubState.READY)
                    .effectiveFrom(LocalDate.now())
                    .currentFlag(true)
                    .createdBy("SYSTEM_FIX")
                    .benchStartDate(LocalDate.now())
                    .build();
            
            benchDetectionRepository.save(newState);
            
            log.info("Successfully created BENCH state for resource {}", resourceId);
            return true;
            
        } catch (Exception e) {
            log.error("Failed to fix state for resource {}: {}", resourceId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Check if a resource has a state record
     */
    public boolean hasState(Long resourceId) {
        return benchDetectionRepository.findCurrentState(resourceId).isPresent();
    }
}
