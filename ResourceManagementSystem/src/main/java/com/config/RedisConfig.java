package com.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {

        RedisStandaloneConfiguration standalone =
                new RedisStandaloneConfiguration("localhost", 6379);

        LettuceClientConfiguration clientConfig =
                LettuceClientConfiguration.builder()
                        .commandTimeout(Duration.ofSeconds(2))
                        .shutdownTimeout(Duration.ofMillis(100))
                        .build();

        LettuceConnectionFactory factory =
                new LettuceConnectionFactory(standalone, clientConfig);

        factory.setShareNativeConnection(false);
        factory.setValidateConnection(false); // IMPORTANT: avoid blocking validation

        // Test connection and log status
        try {
            factory.getConnection().ping();
            log.info("Redis connection established successfully");
        } catch (Exception e) {
            log.warn("Redis connection failed - application will run in degraded mode without caching: {}", e.getMessage());
        }

        return factory;
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        // Create ObjectMapper with JSR310 module for LocalDate support
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        
        // Configure ObjectMapper to handle Spring objects properly
        objectMapper.activateDefaultTyping(
            objectMapper.getPolymorphicTypeValidator(), 
            ObjectMapper.DefaultTyping.NON_FINAL
        );
        
        // Ignore properties that can't be serialized
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        
        // Create custom serializer with the configured ObjectMapper
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(jsonSerializer));

        Map<String, RedisCacheConfiguration> configs = new HashMap<>();

        configs.put("holidays", config.entryTtl(Duration.ofHours(24)));
        configs.put("leaves", config.entryTtl(Duration.ofHours(4)));
        configs.put("active-allocations", config.entryTtl(Duration.ofMinutes(5)));
        configs.put("dashboard-kpis", config.entryTtl(Duration.ofMinutes(15)));
        configs.put("bench-resources", config.entryTtl(Duration.ofMinutes(10)));
        configs.put("resource-skills", config.entryTtl(Duration.ofHours(2)));
        configs.put("resource-timelines", config.entryTtl(Duration.ofMinutes(5)));
        configs.put("demands", config.entryTtl(Duration.ofHours(1)));

        try {
            RedisCacheManager cacheManager = RedisCacheManager.builder(factory)
                    .cacheDefaults(config)
                    .withInitialCacheConfigurations(configs)
                    .build();
            
            log.info("Redis CacheManager initialized successfully");
            return cacheManager;
            
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis CacheManager initialization failed - caching will be disabled: {}", e.getMessage());
            // Return a no-op cache manager that doesn't actually cache
            return createNoOpCacheManager();
        } catch (Exception e) {
            log.error("Unexpected error initializing Redis CacheManager - caching will be disabled: {}", e.getMessage());
            return createNoOpCacheManager();
        }
    }
    
    /**
     * Create a no-op cache manager for graceful degradation when Redis is unavailable.
     */
    private RedisCacheManager createNoOpCacheManager() {
        log.warn("Creating no-op cache manager - application will run without caching");
        
        // Return a cache manager that doesn't actually cache anything
        return RedisCacheManager.builder()
                .cacheDefaults(RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(Duration.ofSeconds(1)) // Very short TTL
                        .disableCachingNullValues())
                .build();
    }

}
