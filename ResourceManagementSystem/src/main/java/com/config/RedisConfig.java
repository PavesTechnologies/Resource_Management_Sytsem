package com.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Configuration
@EnableCaching
public class RedisConfig {

    // Cache TTL configurations
    public static final String DASHBOARD_KPIS_CACHE = "dashboard-kpis";
    public static final String HOLIDAYS_CACHE = "holidays";
    public static final String LEAVES_CACHE = "leaves";
    public static final String ACTIVE_ALLOCATIONS_CACHE = "active-allocations";
    public static final String AVAILABILITY_CALCULATIONS_CACHE = "availability-calculations";
    public static final String RESOURCE_SKILLS_CACHE = "resource-skills";
    public static final String ALLOCATIONS_CACHE = "allocations";
    public static final String BENCH_RESOURCES_CACHE = "bench-resources";
    public static final String BENCH_MATCHES_CACHE = "bench-matches";
    public static final String RESOURCE_TIMELINES_CACHE = "resource-timelines";

    // TTL values
    private static final Duration DASHBOARD_KPIS_TTL = Duration.ofMinutes(15);
    private static final Duration HOLIDAYS_TTL = Duration.ofHours(24);
    private static final Duration LEAVES_TTL = Duration.ofHours(4);
    private static final Duration ACTIVE_ALLOCATIONS_TTL = Duration.ofMinutes(5);
    private static final Duration AVAILABILITY_CALCULATIONS_TTL = Duration.ofMinutes(10);
    private static final Duration RESOURCE_SKILLS_TTL = Duration.ofHours(2);
    private static final Duration ALLOCATIONS_TTL = Duration.ofMinutes(30);
    private static final Duration BENCH_RESOURCES_TTL = Duration.ofMinutes(10);
    private static final Duration BENCH_MATCHES_TTL = Duration.ofMinutes(20);
    private static final Duration RESOURCE_TIMELINES_TTL = Duration.ofMinutes(5);

    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Use String serializer for keys
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringRedisSerializer);
        template.setHashKeySerializer(stringRedisSerializer);

        // Use Jackson serializer for values
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);
        objectMapper.registerModule(new JavaTimeModule());
        jackson2JsonRedisSerializer.setObjectMapper(objectMapper);

        template.setValueSerializer(jackson2JsonRedisSerializer);
        template.setHashValueSerializer(jackson2JsonRedisSerializer);

        template.afterPropertiesSet();
        log.info("Redis template configured with String keys and Jackson JSON values");
        return template;
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // Create default cache configuration
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30)) // Default TTL
                .disableCachingNullValues() // Don't cache null values
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(createJacksonSerializer()));

        // Create cache-specific configurations
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        cacheConfigurations.put(DASHBOARD_KPIS_CACHE, defaultConfig.entryTtl(DASHBOARD_KPIS_TTL));
        cacheConfigurations.put(HOLIDAYS_CACHE, defaultConfig.entryTtl(HOLIDAYS_TTL));
        cacheConfigurations.put(LEAVES_CACHE, defaultConfig.entryTtl(LEAVES_TTL));
        cacheConfigurations.put(ACTIVE_ALLOCATIONS_CACHE, defaultConfig.entryTtl(ACTIVE_ALLOCATIONS_TTL));
        cacheConfigurations.put(AVAILABILITY_CALCULATIONS_CACHE, defaultConfig.entryTtl(AVAILABILITY_CALCULATIONS_TTL));
        cacheConfigurations.put(RESOURCE_SKILLS_CACHE, defaultConfig.entryTtl(RESOURCE_SKILLS_TTL));
        cacheConfigurations.put(ALLOCATIONS_CACHE, defaultConfig.entryTtl(ALLOCATIONS_TTL));
        cacheConfigurations.put(BENCH_RESOURCES_CACHE, defaultConfig.entryTtl(BENCH_RESOURCES_TTL));
        cacheConfigurations.put(BENCH_MATCHES_CACHE, defaultConfig.entryTtl(BENCH_MATCHES_TTL));
        cacheConfigurations.put(RESOURCE_TIMELINES_CACHE, defaultConfig.entryTtl(RESOURCE_TIMELINES_TTL));

        RedisCacheManager cacheManager = RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware() // Enable transaction awareness
                .build();

        log.info("Redis cache manager configured with {} custom cache configurations", cacheConfigurations.size());
        return cacheManager;
    }

    private Jackson2JsonRedisSerializer<Object> createJacksonSerializer() {
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);
        objectMapper.registerModule(new JavaTimeModule());
        serializer.setObjectMapper(objectMapper);
        return serializer;
    }

    /**
     * Cache key generation utility methods following the naming convention:
     * rms:{module}:{entity}:{identifier}:{qualifiers}
     */
    public static class CacheKeyBuilder {
        
        public static String holidaysYear(Integer year) {
            return String.format("rms:holidays:year:%d", year);
        }

        public static String leavesResourceYear(Long resourceId, Integer year) {
            return String.format("rms:leaves:resource:%d:year:%d", resourceId, year);
        }

        public static String allocationsActiveResourceDate(Long resourceId, String date) {
            return String.format("rms:allocations:active:resource:%d:date:%s", resourceId, date);
        }

        public static String availabilityResourceMonth(Long resourceId, String yearMonth) {
            return String.format("rms:availability:resource:%d:month:%s", resourceId, yearMonth);
        }

        public static String dashboardKpis(String hash) {
            return String.format("rms:dashboard:kpis:%s", hash);
        }

        public static String benchResourcesDate(String date) {
            return String.format("rms:bench:resources:date:%s", date);
        }

        public static String benchMatchesSkill(String skill, Integer minExp) {
            return String.format("rms:bench-matches:skill:%s:minExp:%d", skill, minExp);
        }

        public static String resourceTimelineWindow(String hash) {
            return String.format("rms:timeline:window:%s", hash);
        }

        public static String allocation(UUID allocationId) {
            return String.format("rms:allocation:%s", allocationId);
        }

        public static String resource(Long resourceId) {
            return String.format("rms:resource:%d", resourceId);
        }

        public static String demand(Long demandId) {
            return String.format("rms:demand:%d", demandId);
        }

        public static String resourceSkills(Long resourceId) {
            return String.format("rms:skills:resource:%d", resourceId);
        }

        public static String project(Long projectId) {
            return String.format("rms:project:%d", projectId);
        }
    }
}
