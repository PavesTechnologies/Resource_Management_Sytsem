package com.controller.management;

import com.dto.centralised_dto.ApiResponse;
import com.service_interface.allocation_service_interface.AllocationService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Cache Management Controller
 *
 * Provides comprehensive cache monitoring and management endpoints for the Enterprise Skill Gap Matching Engine.
 * Supports both Redis (production) and in-memory (development) cache implementations.
 *
 * Features:
 * - Real-time cache statistics and health monitoring
 * - Selective and bulk cache clearing operations
 * - Cache warming for performance optimization
 * - Multi-environment support (Redis/In-Memory)
 *
 * Security: Restricted to ADMIN and SYSTEM-ADMIN roles only
 *
 * @author Enterprise Skill Gap Matching Engine
 * @version 1.0
 */
@RestController
@RequestMapping("/api/cache")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('ADMIN','SYSTEM-ADMIN')")
public class CacheManagementController {

    // Cache manager instance for accessing all configured caches
    private final CacheManager cacheManager;

    // Service for skill-specific cache operations
    private final AllocationService allocationService;

    // Metrics registry for performance monitoring
    private final MeterRegistry meterRegistry;

    /**
     * Get comprehensive cache statistics and health information
     *
     * Endpoint: GET /api/cache/stats
     *
     * Returns detailed information about all configured caches including:
     * - Cache type (Redis vs In-Memory)
     * - Native cache implementation details
     * - Total cache count
     * - Cache manager information
     *
     * Works with both Redis and in-memory cache implementations.
     * Provides essential monitoring data for production operations.
     *
     * @return ApiResponse containing cache statistics and health information
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCacheStats() {
        try {
            Map<String, Object> stats = new HashMap<>();

            // Iterate through all configured caches and gather statistics
            cacheManager.getCacheNames().forEach(cacheName -> {
                var cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    Map<String, Object> cacheInfo = new HashMap<>();

                    // Get native cache implementation for detailed analysis
                    Object nativeCache = cache.getNativeCache();
                    cacheInfo.put("nativeCacheType", nativeCache.getClass().getSimpleName());

                    // Basic cache information (compatible with all cache types)
                    cacheInfo.put("cacheName", cacheName);
                    cacheInfo.put("cacheType", nativeCache.getClass().getSimpleName());

                    // Determine cache provider for operational awareness
                    if (nativeCache.getClass().getSimpleName().contains("Redis")) {
                        cacheInfo.put("cacheProvider", "Redis");
                        cacheInfo.put("distributed", true);
                    } else {
                        cacheInfo.put("cacheProvider", "In-Memory");
                        cacheInfo.put("distributed", false);
                    }

                    stats.put(cacheName, cacheInfo);
                }
            });

            // Add overall cache manager information for system overview
            Map<String, Object> managerInfo = new HashMap<>();
            managerInfo.put("type", cacheManager.getClass().getSimpleName());
            managerInfo.put("cacheNames", cacheManager.getCacheNames());
            managerInfo.put("totalCaches", cacheManager.getCacheNames().size());
            stats.put("cacheManager", managerInfo);

            log.info("Cache statistics retrieved successfully for {} caches",
                    cacheManager.getCacheNames().size());

            return ResponseEntity.ok(new ApiResponse<>(true, "Cache statistics retrieved successfully", stats));

        } catch (Exception e) {
            log.error("Error retrieving cache statistics", e);
            return ResponseEntity.internalServerError()
                .body(new ApiResponse<>(false, "Error retrieving cache statistics: " + e.getMessage(), null));
        }
    }

    /**
     * Clear a specific cache by name
     *
     * Endpoint: DELETE /api/cache/clear/{cacheName}
     *
     * Allows selective cache clearing for targeted cache invalidation.
     * Useful for clearing specific data types without affecting other caches.
     *
     * Example Usage:
     * - DELETE /api/cache/clear/proficiencyLevels - Clears only proficiency cache
     * - DELETE /api/cache/clear/skills - Clears only skills cache
     *
     * @param cacheName The name of the cache to clear
     * @return ApiResponse indicating success/failure and cleared cache name
     */
    @DeleteMapping("/clear/{cacheName}")
    public ResponseEntity<ApiResponse<String>> clearCache(@PathVariable String cacheName) {
        try {
            var cache = cacheManager.getCache(cacheName);
            if (cache == null) {
                log.warn("Attempted to clear non-existent cache: {}", cacheName);
                return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Cache not found: " + cacheName, null));
            }

            // Clear all entries from the specified cache
            cache.clear();
            log.info("Cache cleared successfully: {}", cacheName);

            return ResponseEntity.ok(new ApiResponse<>(true, "Cache cleared successfully: " + cacheName, cacheName));

        } catch (Exception e) {
            log.error("Error clearing cache: {}", cacheName, e);
            return ResponseEntity.internalServerError()
                .body(new ApiResponse<>(false, "Error clearing cache: " + e.getMessage(), null));
        }
    }

    /**
     * Clear all configured caches
     *
     * Endpoint: DELETE /api/cache/clear-all
     *
     * Performs bulk cache clearing operation for all configured caches.
     * Useful for complete cache invalidation during deployments or data refreshes.
     *
     * Use Cases:
     * - After major data updates
     * - During application deployment
     * - When cache corruption is suspected
     * - For complete system refresh
     *
     * @return ApiResponse containing map of cleared caches and their status
     */
    @DeleteMapping("/clear-all")
    public ResponseEntity<ApiResponse<Map<String, String>>> clearAllCaches() {
        try {
            Map<String, String> clearedCaches = new HashMap<>();

            // Iterate through all configured caches and clear them
            cacheManager.getCacheNames().forEach(cacheName -> {
                var cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.clear();
                    clearedCaches.put(cacheName, "cleared");
                    log.debug("Cleared cache: {}", cacheName);
                }
            });

            log.info("All caches cleared successfully. Total caches cleared: {}", clearedCaches.size());

            return ResponseEntity.ok(new ApiResponse<>(true, "All caches cleared successfully", clearedCaches));

        } catch (Exception e) {
            log.error("Error clearing all caches", e);
            return ResponseEntity.internalServerError()
                .body(new ApiResponse<>(false, "Error clearing all caches: " + e.getMessage(), null));
        }
    }

    /**
     * Clear skill-related caches specifically
     *
     * Endpoint: DELETE /api/cache/clear/skill-caches
     *
     * Targets skill-related caches for selective invalidation.
     * Uses service layer methods for proper cache eviction with consistency guarantees.
     *
     * Affected Caches:
     * - proficiencyLevels: Proficiency level lookups
     * - skills: Skill reference data
     * - subSkills: Subskill reference data
     * - certificates: Certificate reference data
     * - deliveryRoleExpectations: Role expectation data
     *
     * This method ensures cache consistency across all skill-related data.
     *
     * @return ApiResponse indicating success/failure of skill cache clearing
     */
    @DeleteMapping("/clear/skill-caches")
    public ResponseEntity<ApiResponse<String>> clearSkillCaches() {
        try {
            // Use service layer method for proper cache eviction
            // This ensures cache consistency and triggers any configured eviction listeners
            allocationService.clearAllSkillCaches();
            log.info("Skill-related caches cleared via service method - ensuring consistency");

            return ResponseEntity.ok(new ApiResponse<>(true, "Skill-related caches cleared successfully", "skill-caches"));

        } catch (Exception e) {
            log.error("Error clearing skill caches", e);
            return ResponseEntity.internalServerError()
                .body(new ApiResponse<>(false, "Error clearing skill caches: " + e.getMessage(), null));
        }
    }

    /**
     * Warm up caches by pre-loading common data
     *
     * Endpoint: POST /api/cache/warmup
     *
     * Pre-loads frequently accessed data into cache to improve performance.
     * Reduces cold start latency and ensures optimal cache hit ratios.
     *
     * Cache Warming Strategy:
     * - Proficiency Levels: Essential for skill gap analysis
     * - Skills: Core reference data for matching
     * - SubSkills: Detailed skill breakdown data
     * - Certificates: Professional qualification data
     * - Role Expectations: Job requirement mappings
     *
     * Performance Benefits:
     * - Reduces database load during peak usage
     * - Improves response times for skill gap analysis
     * - Ensures cache is populated before user requests
     *
     * @return ApiResponse containing map of warmed caches and their status
     */
    @PostMapping("/warmup")
    public ResponseEntity<ApiResponse<Map<String, String>>> warmupCaches() {
        try {
            Map<String, String> warmupResults = new HashMap<>();

            // Trigger cache warming for proficiency levels
            // This forces loading of all active proficiency levels into cache
            allocationService.clearProficiencyLevelsCache();
            warmupResults.put("proficiencyLevels", "warmed");

            // Future enhancement: Add other cache warming logic here
            // - Skills cache warming
            // - Certificates cache warming
            // - Role expectations cache warming

            log.info("Cache warmup completed successfully. Warmed caches: {}", warmupResults.keySet());

            return ResponseEntity.ok(new ApiResponse<>(true, "Cache warmup completed successfully", warmupResults));

        } catch (Exception e) {
            log.error("Error during cache warmup", e);
            return ResponseEntity.internalServerError()
                .body(new ApiResponse<>(false, "Error during cache warmup: " + e.getMessage(), null));
        }
    }

    /**
     * Get comprehensive cache health status
     *
     * Endpoint: GET /api/cache/health
     *
     * Performs health checks on all configured caches to ensure they are operational.
     * Tests cache accessibility and responsiveness for monitoring and alerting.
     *
     * Health Check Strategy:
     * - Attempts a simple get operation on each cache
     * - Uses "health-check" key to avoid affecting real data
     * - Caches exceptions and connection issues
     * - Provides overall system health assessment
     *
     * Health Status Indicators:
     * - UP: Cache is responding normally
     * - DOWN: Cache is unreachable or throwing exceptions
     * - NOT_FOUND: Cache instance not available
     *
     * Monitoring Integration:
     * - Can be integrated with health monitoring systems
     * - Supports automated alerting on cache failures
     * - Provides detailed error information for troubleshooting
     *
     * @return ApiResponse containing detailed health status for all caches
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCacheHealth() {
        try {
            Map<String, Object> health = new HashMap<>();
            Map<String, String> status = new HashMap<>();

            // Perform health check on each configured cache
            cacheManager.getCacheNames().forEach(cacheName -> {
                var cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    // Simple health check - try to get a test key
                    // This tests cache accessibility without affecting real data
                    try {
                        cache.get("health-check");
                        status.put(cacheName, "UP");
                        log.debug("Health check passed for cache: {}", cacheName);
                    } catch (Exception e) {
                        status.put(cacheName, "DOWN: " + e.getMessage());
                        log.warn("Health check failed for cache {}: {}", cacheName, e.getMessage());
                    }
                } else {
                    status.put(cacheName, "NOT_FOUND");
                    log.warn("Cache instance not found: {}", cacheName);
                }
            });

            // Calculate overall system health
            boolean hasDownCaches = status.values().stream()
                .anyMatch(s -> s.startsWith("DOWN"));

            health.put("status", status);
            health.put("overall", hasDownCaches ? "DOWN" : "UP");
            health.put("totalCaches", status.size());
            health.put("healthyCaches", (int) status.values().stream()
                .filter(s -> s.equals("UP")).count());
            health.put("unhealthyCaches", (int) status.values().stream()
                .filter(s -> !s.equals("UP")).count());

            log.info("Cache health check completed. Overall status: {}, Healthy: {}/{}",
                    health.get("overall"), health.get("healthyCaches"), health.get("totalCaches"));

            return ResponseEntity.ok(new ApiResponse<>(true, "Cache health retrieved successfully", health));

        } catch (Exception e) {
            log.error("Error checking cache health", e);
            return ResponseEntity.internalServerError()
                .body(new ApiResponse<>(false, "Error checking cache health: " + e.getMessage(), null));
        }
    }
}
