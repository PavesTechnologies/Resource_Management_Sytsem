package com.service_imple.bench_service_impl;

import com.repo.bench_repo.BenchDetectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Service to proactively initialize resource states
 * Fixes the "No value present" error by ensuring all eligible resources have state records
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ResourceStateInitializationService {

    private final BenchDetectionRepository benchDetectionRepository;
    private final BenchService benchService;

    /**
     * Initialize resource states on application startup
     * This fixes any existing resources without states
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initializeResourceStatesOnStartup() {
        log.info("Starting resource state initialization on application startup");
        
        try {
            // Find all resources that should have states but don't
            List<Long> resourcesWithoutStates = findResourcesWithoutStates();
            
            log.info("Found {} resources without state records", resourcesWithoutStates.size());
            
            int successCount = 0;
            int failureCount = 0;
            
            for (Long resourceId : resourcesWithoutStates) {
                try {
                    benchService.createOrUpdateBenchState(resourceId);
                    successCount++;
                    log.debug("Successfully initialized state for resource {}", resourceId);
                } catch (Exception e) {
                    failureCount++;
                    log.error("Failed to initialize state for resource {}: {}", resourceId, e.getMessage());
                }
            }
            
            log.info("Resource state initialization completed. Success: {}, Failures: {}", 
                    successCount, failureCount);
            
            if (failureCount > 0) {
                log.warn("Some resources could not be initialized. Manual intervention may be required.");
            }
            
        } catch (Exception e) {
            log.error("Resource state initialization failed: {}", e.getMessage(), e);
            // Don't fail the application startup
        }
    }

    /**
     * Daily scheduled job to ensure all resources have proper states
     * This prevents future "No value present" errors
     */
//    @Scheduled(cron = "0 0 2 * * ?") // Daily at 2 AM
    @Transactional
    public void dailyResourceStateCheck() {
        log.info("Starting daily resource state check");
        
        try {
            List<Long> resourcesWithoutStates = findResourcesWithoutStates();
            
            if (!resourcesWithoutStates.isEmpty()) {
                log.info("Found {} resources without states during daily check", resourcesWithoutStates.size());
                
                for (Long resourceId : resourcesWithoutStates) {
                    try {
                        benchService.createOrUpdateBenchState(resourceId);
                        log.info("Fixed missing state for resource {} during daily check", resourceId);
                    } catch (Exception e) {
                        log.error("Failed to fix state for resource {} during daily check: {}", 
                                resourceId, e.getMessage());
                    }
                }
            } else {
                log.info("All resources have proper states - no action needed");
            }
            
        } catch (Exception e) {
            log.error("Daily resource state check failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Find resources that should have states but don't
     * These are eligible resources that are missing from resource_state table
     */
    private List<Long> findResourcesWithoutStates() {
        // Find resources eligible for bench but without any state record
        return benchDetectionRepository.findBenchEligibleResources(LocalDate.now())
                .stream()
                .filter(resourceId -> {
                    // Check if resource has no current state
                    return !benchDetectionRepository.findCurrentState(resourceId).isPresent();
                })
                .toList();
    }

    /**
     * Manual trigger to fix specific resource
     * Can be called from admin endpoints or for troubleshooting
     */
    @Transactional
    public boolean fixResourceState(Long resourceId) {
        log.info("Attempting to fix state for resource {}", resourceId);
        
        try {
            if (!benchDetectionRepository.findCurrentState(resourceId).isPresent()) {
                benchService.createOrUpdateBenchState(resourceId);
                log.info("Successfully fixed state for resource {}", resourceId);
                return true;
            } else {
                log.info("Resource {} already has a state record", resourceId);
                return false;
            }
        } catch (Exception e) {
            log.error("Failed to fix state for resource {}: {}", resourceId, e.getMessage());
            return false;
        }
    }
}
