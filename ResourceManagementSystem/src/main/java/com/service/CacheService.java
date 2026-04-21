package com.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Get value from cache
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                log.debug("Cache hit for key: {}", key);
                return (T) value;
            } else {
                log.debug("Cache miss for key: {}", key);
                return null;
            }
        } catch (Exception e) {
            log.error("Error getting value from cache for key: {}", key, e);
            return null;
        }
    }

    /**
     * Put value in cache with default TTL
     */
    public void put(String key, Object value) {
        put(key, value, Duration.ofMinutes(30)); // Default TTL
    }

    /**
     * Put value in cache with custom TTL
     */
    public void put(String key, Object value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, value, ttl.toMillis(), TimeUnit.MILLISECONDS);
            log.debug("Cached value for key: {} with TTL: {}ms", key, ttl.toMillis());
        } catch (Exception e) {
            log.error("Error putting value in cache for key: {}", key, e);
        }
    }

    /**
     * Evict specific key from cache
     */
    public void evict(String key) {
        try {
            Boolean result = redisTemplate.delete(key);
            if (Boolean.TRUE.equals(result)) {
                log.debug("Evicted cache key: {}", key);
            } else {
                log.debug("Cache key not found for eviction: {}", key);
            }
        } catch (Exception e) {
            log.error("Error evicting cache key: {}", key, e);
        }
    }

    /**
     * Evict multiple keys from cache
     */
    public void evict(Collection<String> keys) {
        try {
            Long deletedCount = redisTemplate.delete(keys);
            log.debug("Evicted {} cache keys", deletedCount);
        } catch (Exception e) {
            log.error("Error evicting cache keys: {}", keys, e);
        }
    }

    /**
     * Evict keys matching a pattern
     * Use with caution - can be expensive on large datasets
     */
    public void evictByPattern(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                Long deletedCount = redisTemplate.delete(keys);
                log.debug("Evicted {} cache keys matching pattern: {}", deletedCount, pattern);
            } else {
                log.debug("No cache keys found matching pattern: {}", pattern);
            }
        } catch (Exception e) {
            log.error("Error evicting cache keys by pattern: {}", pattern, e);
        }
    }

    /**
     * Check if key exists in cache
     */
    public boolean exists(String key) {
        try {
            Boolean result = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("Error checking if cache key exists: {}", key, e);
            return false;
        }
    }

    /**
     * Set TTL for existing key
     */
    public void setTtl(String key, Duration ttl) {
        try {
            Boolean result = redisTemplate.expire(key, ttl.toMillis(), TimeUnit.MILLISECONDS);
            if (Boolean.TRUE.equals(result)) {
                log.debug("Set TTL for cache key: {} to {}ms", key, ttl.toMillis());
            } else {
                log.debug("Failed to set TTL for cache key: {} (key may not exist)", key);
            }
        } catch (Exception e) {
            log.error("Error setting TTL for cache key: {}", key, e);
        }
    }

    /**
     * Get remaining TTL for key
     */
    public Duration getTtl(String key) {
        try {
            Long ttl = redisTemplate.getExpire(key, TimeUnit.MILLISECONDS);
            if (ttl != null && ttl >= 0) {
                return Duration.ofMillis(ttl);
            }
            return Duration.ZERO; // Key doesn't exist or has no expiration
        } catch (Exception e) {
            log.error("Error getting TTL for cache key: {}", key, e);
            return Duration.ZERO;
        }
    }

    /**
     * Increment numeric value in cache
     */
    public Long increment(String key) {
        return increment(key, 1L);
    }

    /**
     * Increment numeric value in cache by delta
     */
    public Long increment(String key, long delta) {
        try {
            Long result = redisTemplate.opsForValue().increment(key, delta);
            log.debug("Incremented cache key: {} by {} to {}", key, delta, result);
            return result;
        } catch (Exception e) {
            log.error("Error incrementing cache key: {}", key, e);
            return null;
        }
    }

    /**
     * Add value to set
     */
    public void addToSet(String key, Object value) {
        try {
            Long result = redisTemplate.opsForSet().add(key, value);
            log.debug("Added value to set key: {} (added: {} new elements)", key, result);
        } catch (Exception e) {
            log.error("Error adding value to set key: {}", key, e);
        }
    }

    /**
     * Get all values from set
     */
    @SuppressWarnings("unchecked")
    public <T> Set<T> getSet(String key) {
        try {
            Set<Object> values = redisTemplate.opsForSet().members(key);
            if (values != null) {
                log.debug("Retrieved {} values from set key: {}", values.size(), key);
                return (Set<T>) values;
            }
            return Set.of();
        } catch (Exception e) {
            log.error("Error getting values from set key: {}", key, e);
            return Set.of();
        }
    }

    /**
     * Remove value from set
     */
    public void removeFromSet(String key, Object value) {
        try {
            Long result = redisTemplate.opsForSet().remove(key, value);
            log.debug("Removed value from set key: {} (removed: {} elements)", key, result);
        } catch (Exception e) {
            log.error("Error removing value from set key: {}", key, e);
        }
    }

    /**
     * Add value to list (right push)
     */
    public void addToList(String key, Object value) {
        try {
            Long result = redisTemplate.opsForList().rightPush(key, value);
            log.debug("Added value to list key: {} (new size: {})", key, result);
        } catch (Exception e) {
            log.error("Error adding value to list key: {}", key, e);
        }
    }

    /**
     * Get all values from list
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> getList(String key) {
        try {
            List<Object> values = redisTemplate.opsForList().range(key, 0, -1);
            if (values != null) {
                log.debug("Retrieved {} values from list key: {}", values.size(), key);
                return (List<T>) values;
            }
            return List.of();
        } catch (Exception e) {
            log.error("Error getting values from list key: {}", key, e);
            return List.of();
        }
    }

    /**
     * Get list size
     */
    public Long getListSize(String key) {
        try {
            Long size = redisTemplate.opsForList().size(key);
            log.debug("List key: {} has size: {}", key, size);
            return size;
        } catch (Exception e) {
            log.error("Error getting size of list key: {}", key, e);
            return 0L;
        }
    }

    /**
     * Clear all cache (use with extreme caution)
     */
    public void clearAll() {
        try {
            Set<String> keys = redisTemplate.keys("*");
            if (keys != null && !keys.isEmpty()) {
                Long deletedCount = redisTemplate.delete(keys);
                log.warn("Cleared all cache - deleted {} keys", deletedCount);
            } else {
                log.info("No cache keys found to clear");
            }
        } catch (Exception e) {
            log.error("Error clearing all cache", e);
        }
    }

    /**
     * Get cache statistics (keys count)
     */
    public long getCacheSize() {
        try {
            Set<String> keys = redisTemplate.keys("*");
            return keys != null ? keys.size() : 0;
        } catch (Exception e) {
            log.error("Error getting cache size", e);
            return 0;
        }
    }
}
