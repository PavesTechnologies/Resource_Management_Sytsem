package com.service;

import com.config.RedisConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheEvictionService {

    private final CacheService cacheService;

    // ==================== ALLOCATION EVICTION ====================

    /**
     * Evict all allocation-related caches when allocation changes
     */
    public void evictAllocationCaches(UUID allocationId, Long resourceId) {
        List<String> keysToEvict = new ArrayList<>();
        
        // Specific allocation
        keysToEvict.add(RedisConfig.CacheKeyBuilder.allocation(allocationId));
        
        // Active allocations for resource (pattern-based)
        keysToEvict.add("rms:allocations:active:resource:" + resourceId + ":*");
        
        // Resource-related caches
        keysToEvict.addAll(getResourceRelatedKeys(resourceId));
        
        // Dashboard and bench caches
        keysToEvict.addAll(getDashboardAndBenchKeys());
        
        evictMultipleKeys(keysToEvict, "allocation change");
    }

    /**
     * Evict allocation caches using annotation (for specific allocation)
     */
    @CacheEvict(value = RedisConfig.ALLOCATIONS_CACHE, key = "#allocationId")
    public void evictAllocationById(UUID allocationId) {
        log.info("Evicted allocation cache for ID: {}", allocationId);
    }

    // ==================== RESOURCE EVICTION ====================

    /**
     * Evict all resource-related caches when resource changes
     */
    public void evictResourceCaches(Long resourceId) {
        List<String> keysToEvict = getResourceRelatedKeys(resourceId);
        keysToEvict.addAll(getDashboardAndBenchKeys());
        
        evictMultipleKeys(keysToEvict, "resource change");
    }

    /**
     * Get all keys related to a specific resource
     */
    private List<String> getResourceRelatedKeys(Long resourceId) {
        List<String> keys = new ArrayList<>();
        
        // Resource basic data
        keys.add(RedisConfig.CacheKeyBuilder.resource(resourceId));
        
        // Resource skills
        keys.add(RedisConfig.CacheKeyBuilder.resourceSkills(resourceId));
        
        // Resource leaves (all years)
        keys.add("rms:leaves:resource:" + resourceId + ":*");
        
        // Resource availability calculations
        keys.add("rms:availability:resource:" + resourceId + ":*");
        
        // Resource timeline windows
        keys.add("rms:timeline:window:*resource:" + resourceId + "*");
        
        // Active allocations (all dates)
        keys.add("rms:allocations:active:resource:" + resourceId + ":*");
        
        return keys;
    }

    // ==================== DEMAND EVICTION ====================

    /**
     * Evict all demand-related caches when demand changes
     */
    public void evictDemandCaches(Long demandId) {
        List<String> keysToEvict = new ArrayList<>();
        
        // Specific demand
        keysToEvict.add(RedisConfig.CacheKeyBuilder.demand(demandId));
        
        // Bench matching (all skills/experience levels)
        keysToEvict.add("rms:bench-matches:*");
        
        // Dashboard and bench caches
        keysToEvict.addAll(getDashboardAndBenchKeys());
        
        evictMultipleKeys(keysToEvict, "demand change");
    }

    /**
     * Evict demand cache using annotation
     */
    @CacheEvict(value = "demands", key = "#demandId")
    public void evictDemandById(Long demandId) {
        log.info("Evicted demand cache for ID: {}", demandId);
    }

    // ==================== SKILL EVICTION ====================

    /**
     * Evict all skill-related caches when skills change
     */
    public void evictSkillCaches(Long resourceId) {
        List<String> keysToEvict = new ArrayList<>();
        
        // Resource skills
        keysToEvict.add(RedisConfig.CacheKeyBuilder.resourceSkills(resourceId));
        
        // Bench matching (all skills)
        keysToEvict.add("rms:bench-matches:*");
        
        // Dashboard and bench caches
        keysToEvict.addAll(getDashboardAndBenchKeys());
        
        evictMultipleKeys(keysToEvict, "skill change");
    }

    // ==================== PROJECT EVICTION ====================

    /**
     * Evict all project-related caches when project changes
     */
    public void evictProjectCaches(Long projectId) {
        List<String> keysToEvict = new ArrayList<>();
        
        // Specific project
        keysToEvict.add(RedisConfig.CacheKeyBuilder.project(projectId));
        
        // Dashboard and bench caches
        keysToEvict.addAll(getDashboardAndBenchKeys());
        
        evictMultipleKeys(keysToEvict, "project change");
    }

    // ==================== HOLIDAY/LEAVE EVICTION ====================

    /**
     * Evict holiday caches when holiday data changes
     */
    @Caching(evict = {
        @CacheEvict(value = RedisConfig.HOLIDAYS_CACHE, allEntries = true)
    })
    public void evictHolidayCaches() {
        log.info("Evicted all holiday caches due to holiday data change");
        
        // Also evict availability calculations and dashboard
        List<String> keysToEvict = new ArrayList<>();
        keysToEvict.add("rms:availability:*");
        keysToEvict.addAll(getDashboardAndBenchKeys());
        
        evictMultipleKeys(keysToEvict, "holiday data change");
    }

    /**
     * Evict leave caches for specific resource
     */
    public void evictLeaveCaches(Long resourceId) {
        List<String> keysToEvict = new ArrayList<>();
        
        // Resource leaves (all years)
        keysToEvict.add("rms:leaves:resource:" + resourceId + ":*");
        
        // Resource availability calculations
        keysToEvict.add("rms:availability:resource:" + resourceId + ":*");
        
        // Dashboard and bench caches
        keysToEvict.addAll(getDashboardAndBenchKeys());
        
        evictMultipleKeys(keysToEvict, "leave data change");
    }

    // ==================== BULK EVICTION ====================

    /**
     * Evict all dashboard and bench-related caches
     */
    public void evictDashboardAndBenchCaches() {
        List<String> keysToEvict = getDashboardAndBenchKeys();
        evictMultipleKeys(keysToEvict, "dashboard/bench refresh");
    }

    /**
     * Evict all availability calculation caches
     */
    public void evictAvailabilityCaches() {
        cacheService.evictByPattern("rms:availability:*");
        log.info("Evicted all availability calculation caches");
    }

    /**
     * Evict all bench-related caches
     */
    public void evictBenchCaches() {
        cacheService.evictByPattern("rms:bench:*");
        log.info("Evicted all bench-related caches");
    }

    /**
     * Evict all timeline caches
     */
    public void evictTimelineCaches() {
        cacheService.evictByPattern("rms:timeline:*");
        log.info("Evicted all timeline caches");
    }

    /**
     * Evict timeline caches for specific resource
     */
    public void evictTimelineCachesForResource(Long resourceId) {
        cacheService.evictByPattern("rms:timeline:window:*");
        log.info("Evicted timeline caches for resource: {}", resourceId);
    }

    /**
     * Evict timeline caches for allocation changes
     */
    public void evictTimelineCachesForAllocationChange(Long resourceId) {
        // Evict all timeline caches since allocations affect timeline data
        evictTimelineCaches();
        log.info("Evicted timeline caches due to allocation change for resource: {}", resourceId);
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Get dashboard and bench related keys
     */
    private List<String> getDashboardAndBenchKeys() {
        List<String> keys = new ArrayList<>();
        
        // Dashboard KPIs (all variations)
        keys.add("rms:dashboard:kpis:*");
        
        // Bench resources (all dates)
        keys.add("rms:bench:resources:*");
        
        // Bench matching (all skills/experience)
        keys.add("rms:bench-matches:*");
        
        return keys;
    }

    /**
     * Evict multiple keys with pattern support
     */
    private void evictMultipleKeys(List<String> keys, String reason) {
        for (String key : keys) {
            if (key.contains("*")) {
                cacheService.evictByPattern(key);
            } else {
                cacheService.evict(key);
            }
        }
        log.info("Evicted {} cache keys due to: {}", keys.size(), reason);
    }

    /**
     * Evict caches by module
     */
    public void evictByModule(String module) {
        String pattern = "rms:" + module + ":*";
        cacheService.evictByPattern(pattern);
        log.info("Evicted all caches for module: {}", module);
    }

    /**
     * Evict caches for multiple resources
     */
    public void evictResourcesCaches(List<Long> resourceIds) {
        List<String> keysToEvict = new ArrayList<>();
        
        for (Long resourceId : resourceIds) {
            keysToEvict.addAll(getResourceRelatedKeys(resourceId));
        }
        
        keysToEvict.addAll(getDashboardAndBenchKeys());
        evictMultipleKeys(keysToEvict, "multiple resource changes");
    }

    /**
     * Evict caches for date range (useful for bulk operations)
     */
    public void evictDateRangeCaches(String startDate, String endDate) {
        List<String> keysToEvict = new ArrayList<>();
        
        // Bench resources for date range
        keysToEvict.add("rms:bench:resources:date:" + startDate + "*");
        
        // Active allocations for date range
        keysToEvict.add("rms:allocations:active:*:date:" + startDate + "*");
        
        // Timeline windows for date range
        keysToEvict.add("rms:timeline:window:*" + startDate + "*");
        
        evictMultipleKeys(keysToEvict, "date range operation");
    }

    /**
     * Comprehensive cache eviction for major data changes
     */
    public void evictAllBusinessCaches() {
        List<String> patterns = new ArrayList<>();
        
        patterns.add("rms:holidays:*");
        patterns.add("rms:leaves:*");
        patterns.add("rms:allocations:*");
        patterns.add("rms:availability:*");
        patterns.add("rms:dashboard:*");
        patterns.add("rms:bench:*");
        patterns.add("rms:timeline:*");
        patterns.add("rms:skills:*");
        
        for (String pattern : patterns) {
            cacheService.evictByPattern(pattern);
        }
        
        log.warn("Evicted all business caches due to major data change");
    }
}
