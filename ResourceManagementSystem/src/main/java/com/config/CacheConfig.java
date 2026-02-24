package com.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cache configuration for performance optimization
 * Enables caching for frequently accessed reference data
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Configure cache manager for proficiency levels and other reference data
     * Uses ConcurrentMapCacheManager for in-memory caching
     */
    @Bean
    public CacheManager cacheManager() {
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
        
        // Enable cache statistics for monitoring
        cacheManager.setCacheNames(java.util.List.of("proficiencyLevels"));
        
        return cacheManager;
    }
}
