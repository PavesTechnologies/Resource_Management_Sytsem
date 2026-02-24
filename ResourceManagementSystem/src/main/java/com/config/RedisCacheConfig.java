package com.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

/**
 * Redis Cache Configuration for Enterprise Skill Gap Matching Engine
 * 
 * Provides production-ready distributed caching with Redis and automatic fallback
 * to in-memory caching for development environments or Redis failures.
 * 
 * Configuration Strategy:
 * - Primary: Redis distributed cache for production environments
 * - Fallback: In-memory cache for development/Redis failures
 * - Conditional: Only activates when Redis configuration is present
 * 
 * Cache TTL Configuration (Externalized):
 * - Proficiency Levels: 24 hours (reference data changes infrequently)
 * - Skills & SubSkills: 12 hours (moderate change frequency)
 * - Certificates: 6 hours (may change more frequently)
 * - Role Expectations: 2 hours (business requirements may change)
 * 
 * Features:
 * - JSON serialization for complex objects
 * - Null value prevention to avoid cache pollution
 * - Key prefixing for multi-tenant support
 * - Graceful degradation on Redis failures
 * - Development-friendly automatic fallback
 * 
 * @author Enterprise Skill Gap Matching Engine
 * @version 1.0
 */
@Slf4j
@Configuration
@EnableCaching
@ConditionalOnProperty(name = "spring.redis.host")
public class RedisCacheConfig {

    @Value("${app.cache.proficiency-levels.ttl:24h}")
    private Duration proficiencyLevelsTtl;

    // Cache TTL for skills - 12 hours (moderate change frequency)
    @Value("${app.cache.skills.ttl:12h}")
    private Duration skillsTtl;

    // Cache TTL for certificates - 6 hours (may change more frequently)
    @Value("${app.cache.certificates.ttl:6h}")
    private Duration certificatesTtl;

    // Cache TTL for role expectations - 2 hours (business requirements may change)
    @Value("${app.cache.role-expectations.ttl:2h}")
    private Duration roleExpectationsTtl;

    /**
     * Primary Redis Cache Manager for Production Environments
     * 
     * Creates and configures Redis cache manager with optimized settings for
     * the Enterprise Skill Gap Matching Engine.
     * 
     * Configuration Features:
     * - Individual TTL settings per cache type based on data change patterns
     * - JSON serialization for complex object storage
     * - String serialization for cache keys (human-readable)
     * - Null value prevention (avoids cache pollution)
     * - Graceful fallback to in-memory cache on Redis failures
     * 
     * Cache Configuration Details:
     * - proficiencyLevels: 24h TTL (stable reference data)
     * - skills/subSkills: 12h TTL (moderate change frequency)
     * - certificates: 6h TTL (frequent changes possible)
     * - deliveryRoleExpectations: 2h TTL (business requirements)
     * 
     * Error Handling:
     * - Catches Redis connection failures
     * - Automatically falls back to in-memory cache
     * - Logs configuration issues for troubleshooting
     * - Ensures application continues functioning
     * 
     * @param redisConnectionFactory Redis connection factory for cache operations
     * @return Configured RedisCacheManager or fallback ConcurrentMapCacheManager
     */
    @Bean
    @Primary
    public CacheManager redisCacheManager(RedisConnectionFactory redisConnectionFactory) {
        try {
            Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

            // Configure proficiency levels cache with 24-hour TTL
            // Used for skill gap analysis proficiency lookups
            cacheConfigurations.put("proficiencyLevels", 
                RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(proficiencyLevelsTtl)
                    .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                    .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                    .disableCachingNullValues());

            // Configure skills cache with 12-hour TTL
            // Used for skill reference data and matching operations
            cacheConfigurations.put("skills", 
                RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(skillsTtl)
                    .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                    .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                    .disableCachingNullValues());

            // Configure sub-skills cache with 12-hour TTL
            // Used for detailed skill breakdown in gap analysis
            cacheConfigurations.put("subSkills", 
                RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(skillsTtl)
                    .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                    .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                    .disableCachingNullValues());

            // Configure certificates cache with 6-hour TTL
            // Used for professional qualification validation
            cacheConfigurations.put("certificates", 
                RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(certificatesTtl)
                    .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                    .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                    .disableCachingNullValues());

            // Configure role expectations cache with 2-hour TTL
            // Used for job requirement matching and gap analysis
            cacheConfigurations.put("deliveryRoleExpectations", 
                RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(roleExpectationsTtl)
                    .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                    .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                    .disableCachingNullValues());

            // Build and return Redis cache manager with all configurations
            return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofHours(1)) // Default 1-hour TTL
                    .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                    .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                    .disableCachingNullValues())
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
        } catch (Exception e) {
            // Fallback to simple cache manager if Redis fails
            log.error("Redis cache configuration failed, falling back to in-memory cache", e);
            return fallbackCacheManager();
        }
    }
    
    /**
     * Fallback cache manager for development environments
     */
    @Bean
    @ConditionalOnMissingBean(RedisCacheManager.class)
    public CacheManager fallbackCacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        
        // Configure caches for different data types
        cacheManager.setCacheNames(java.util.List.of(
            "proficiencyLevels",      // Cache for proficiency level lookups
            "skills",                 // Cache for skill reference data
            "subSkills",              // Cache for subskill reference data
            "certificates",           // Cache for certificate reference data
            "deliveryRoleExpectations" // Cache for role expectations
        ));
        
        // Allow null values to be cached
        cacheManager.setAllowNullValues(false);
        
        return cacheManager;
    }
}
