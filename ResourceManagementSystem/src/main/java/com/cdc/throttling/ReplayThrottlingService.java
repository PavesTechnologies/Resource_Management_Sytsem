package com.cdc.throttling;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ENTERPRISE-GRADE Replay Throttling & Storm Protection Service.
 * 
 * CRITICAL SAFETY MECHANISM to prevent replay storms and system overload
 * during bulk replays, retry cascades, or high-volume CDC events.
 * 
 * Features:
 * - Rate limiting per entity type
 * - Global replay throttling
 * - Storm detection and auto-throttling
 * - Circuit breaker pattern for replay protection
 * - Redis-based distributed throttling
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReplayThrottlingService {

    private final StringRedisTemplate redisTemplate;

    // Rate limiting configuration
    private static final String REPLAY_RATE_PREFIX = "cdc:replay:rate:";
    private static final String STORM_DETECTION_PREFIX = "cdc:storm:detect:";
    private static final String CIRCUIT_BREAKER_PREFIX = "cdc:circuit:breaker:";
    
    // Default throttling limits
    private static final int DEFAULT_REPLAY_RATE_LIMIT = 100; // events per minute
    private static final int STORM_THRESHOLD = 500; // events per minute
    private static final Duration THROTTLE_WINDOW = Duration.ofMinutes(1);
    private static final Duration CIRCUIT_BREAKER_TIMEOUT = Duration.ofMinutes(5);
    
    // Local counters for storm detection
    private final AtomicLong globalReplayCounter = new AtomicLong(0);
    private volatile LocalDateTime lastResetTime = LocalDateTime.now();

    /**
     * Check if replay is allowed for specific entity type.
     * 
     * @param entityType Entity type (e.g., "EOS-employee_details", "PMS-projects")
     * @return Throttling result with allowance status
     */
    public ThrottlingResult checkReplayAllowed(String entityType) {
        try {
            // Check circuit breaker first
            if (isCircuitBreakerOpen(entityType)) {
                return ThrottlingResult.rejected("Circuit breaker is open for " + entityType);
            }
            
            // Check rate limiting
            if (isRateLimitExceeded(entityType)) {
                return ThrottlingResult.rejected("Rate limit exceeded for " + entityType);
            }
            
            // Check for storm conditions
            if (detectStormConditions()) {
                // Trigger circuit breaker if storm detected
                triggerCircuitBreaker(entityType);
                return ThrottlingResult.rejected("Storm conditions detected, circuit breaker triggered for " + entityType);
            }
            
            // Increment counters and allow replay
            incrementReplayCounters(entityType);
            return ThrottlingResult.allowed();
            
        } catch (Exception e) {
            log.error("Error checking replay throttling for {}: {}", entityType, e.getMessage(), e);
            // Fail open - allow replay if throttling service fails
            return ThrottlingResult.allowed();
        }
    }

    /**
     * Record successful replay completion.
     * 
     * @param entityType Entity type
     * @param processingTimeMs Processing time in milliseconds
     */
    public void recordReplayCompletion(String entityType, long processingTimeMs) {
        try {
            // Update processing metrics
            String metricsKey = REPLAY_RATE_PREFIX + entityType + ":metrics";
            String metrics = String.format("%d:%d", System.currentTimeMillis(), processingTimeMs);
            
            // Use Redis list for recent metrics (keep last 100 entries)
            redisTemplate.opsForList().rightPush(metricsKey, metrics);
            redisTemplate.opsForList().trim(metricsKey, 0, 99);
            
            // Set expiration
            redisTemplate.expire(metricsKey, Duration.ofHours(1));
            
        } catch (Exception e) {
            log.error("Error recording replay completion for {}: {}", entityType, e.getMessage(), e);
        }
    }

    /**
     * Reset throttling counters for entity type.
     * 
     * @param entityType Entity type to reset
     */
    public void resetThrottlingCounters(String entityType) {
        try {
            String rateKey = REPLAY_RATE_PREFIX + entityType;
            redisTemplate.delete(rateKey);
            
            // Close circuit breaker if open
            String circuitKey = CIRCUIT_BREAKER_PREFIX + entityType;
            redisTemplate.delete(circuitKey);
            
            log.info("Reset throttling counters for {}", entityType);
            
        } catch (Exception e) {
            log.error("Error resetting throttling counters for {}: {}", entityType, e.getMessage(), e);
        }
    }

    /**
     * Get current throttling statistics.
     * 
     * @param entityType Entity type
     * @return Throttling statistics
     */
    public ThrottlingStatistics getThrottlingStatistics(String entityType) {
        try {
            String rateKey = REPLAY_RATE_PREFIX + entityType;
            String stormKey = STORM_DETECTION_PREFIX + entityType;
            String circuitKey = CIRCUIT_BREAKER_PREFIX + entityType;
            
            Long currentRate = parseLong(redisTemplate.opsForValue().get(rateKey));
            Long stormCount = parseLong(redisTemplate.opsForValue().get(stormKey));
            Boolean circuitOpen = redisTemplate.hasKey(circuitKey);
            
            return ThrottlingStatistics.builder()
                .entityType(entityType)
                .currentRate(currentRate != null ? currentRate : 0)
                .stormCount(stormCount != null ? stormCount : 0)
                .circuitBreakerOpen(circuitOpen)
                .globalReplayCount(globalReplayCounter.get())
                .build();
                
        } catch (Exception e) {
            log.error("Error getting throttling statistics for {}: {}", entityType, e.getMessage(), e);
            return ThrottlingStatistics.empty(entityType);
        }
    }

    /**
     * Check if rate limit is exceeded for entity type.
     */
    private boolean isRateLimitExceeded(String entityType) {
        try {
            String rateKey = REPLAY_RATE_PREFIX + entityType;
            
            // Use Redis INCR with expiration for atomic rate limiting
            Long currentCount = redisTemplate.opsForValue().increment(rateKey);
            
            if (currentCount == 1) {
                // Set expiration for first increment
                redisTemplate.expire(rateKey, THROTTLE_WINDOW);
            }
            
            return currentCount > DEFAULT_REPLAY_RATE_LIMIT;
            
        } catch (Exception e) {
            log.error("Error checking rate limit for {}: {}", entityType, e.getMessage(), e);
            return false; // Fail open
        }
    }

    /**
     * Detect storm conditions globally.
     */
    private boolean detectStormConditions() {
        try {
            // Reset counter if window expired
            LocalDateTime now = LocalDateTime.now();
            if (Duration.between(lastResetTime, now).toMinutes() >= 1) {
                globalReplayCounter.set(0);
                lastResetTime = now;
            }
            
            long currentCount = globalReplayCounter.incrementAndGet();
            
            // Check if storm threshold exceeded
            if (currentCount > STORM_THRESHOLD) {
                log.warn("Replay storm detected! Global count: {}, threshold: {}", currentCount, STORM_THRESHOLD);
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            log.error("Error detecting storm conditions: {}", e.getMessage(), e);
            return false; // Fail open
        }
    }

    /**
     * Check if circuit breaker is open for entity type.
     */
    private boolean isCircuitBreakerOpen(String entityType) {
        try {
            String circuitKey = CIRCUIT_BREAKER_PREFIX + entityType;
            return redisTemplate.hasKey(circuitKey);
            
        } catch (Exception e) {
            log.error("Error checking circuit breaker for {}: {}", entityType, e.getMessage(), e);
            return false; // Fail open
        }
    }

    /**
     * Trigger circuit breaker for entity type.
     */
    private void triggerCircuitBreaker(String entityType) {
        try {
            String circuitKey = CIRCUIT_BREAKER_PREFIX + entityType;
            redisTemplate.opsForValue().set(circuitKey, "OPEN", CIRCUIT_BREAKER_TIMEOUT);
            
            log.warn("Circuit breaker triggered for {} - timeout: {}", entityType, CIRCUIT_BREAKER_TIMEOUT);
            
        } catch (Exception e) {
            log.error("Error triggering circuit breaker for {}: {}", entityType, e.getMessage(), e);
        }
    }

    /**
     * Increment replay counters.
     */
    private void incrementReplayCounters(String entityType) {
        try {
            // Update storm detection counter
            String stormKey = STORM_DETECTION_PREFIX + entityType;
            redisTemplate.opsForValue().increment(stormKey);
            redisTemplate.expire(stormKey, THROTTLE_WINDOW);
            
        } catch (Exception e) {
            log.error("Error incrementing replay counters for {}: {}", entityType, e.getMessage(), e);
        }
    }

    /**
     * Throttling result.
     */
    public static class ThrottlingResult {
        private final boolean allowed;
        private final String reason;

        private ThrottlingResult(boolean allowed, String reason) {
            this.allowed = allowed;
            this.reason = reason;
        }

        public static ThrottlingResult allowed() {
            return new ThrottlingResult(true, null);
        }

        public static ThrottlingResult rejected(String reason) {
            return new ThrottlingResult(false, reason);
        }

        public boolean isAllowed() {
            return allowed;
        }

        public String getReason() {
            return reason;
        }
    }

    /**
     * Throttling statistics.
     */
    @lombok.Builder
    @lombok.Data
    public static class ThrottlingStatistics {
        private String entityType;
        private long currentRate;
        private long stormCount;
        private boolean circuitBreakerOpen;
        private long globalReplayCount;

        public static ThrottlingStatistics empty(String entityType) {
            return ThrottlingStatistics.builder()
                .entityType(entityType)
                .currentRate(0)
                .stormCount(0)
                .circuitBreakerOpen(false)
                .globalReplayCount(0)
                .build();
        }
    }

    /**
     * Safely parse String to Long with null handling.
     */
    private Long parseLong(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            log.warn("Failed to parse Long value from Redis: {}", value, e);
            return null;
        }
    }
}
