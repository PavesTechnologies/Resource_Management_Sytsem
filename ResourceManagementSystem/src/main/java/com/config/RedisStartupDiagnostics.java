package com.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisStartupDiagnostics {

    private static final String DIAGNOSTIC_KEY = "rms:redis:start-up:diagnostic";

    private final RedisProperties redisProperties;
    private final RedisConnectionFactory redisConnectionFactory;
    private final StringRedisTemplate stringRedisTemplate;
    @Qualifier("cacheManager")
    private final CacheManager cacheManager;

    @EventListener(ApplicationReadyEvent.class)
    public void verifyRedisAfterStartup() {
        log.info(
                "Redis startup diagnostics: host={}, port={}, database={}, sslEnabled={}, timeout={}, connectTimeout={}",
                redisProperties.getHost(),
                redisProperties.getPort(),
                redisProperties.getDatabase(),
                redisProperties.getSsl() != null && redisProperties.getSsl().isEnabled(),
                redisProperties.getTimeout(),
                redisProperties.getConnectTimeout()
        );

        if (redisConnectionFactory instanceof LettuceConnectionFactory lettuceConnectionFactory) {
            log.info(
                    "Redis connection factory state: started={}, shareNativeConnection={}, validateConnection={}",
                    lettuceConnectionFactory.isRunning(),
                    lettuceConnectionFactory.getShareNativeConnection(),
                    lettuceConnectionFactory.getValidateConnection()
            );
        } else {
            log.info("Redis connection factory type: {}", redisConnectionFactory.getClass().getName());
        }

        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            String ping = connection.ping();
            log.info("Redis PING successful: {}", ping);
        } catch (Exception ex) {
            log.warn("Redis connection failed after application startup - application will run in degraded mode without caching: {}", ex.getMessage());
            return;
        }

        try {
            stringRedisTemplate.opsForValue().set(DIAGNOSTIC_KEY, "ok", Duration.ofSeconds(30));
            String value = stringRedisTemplate.opsForValue().get(DIAGNOSTIC_KEY);
            stringRedisTemplate.delete(DIAGNOSTIC_KEY);
            log.info("RedisTemplate round-trip successful: {}", value);
        } catch (Exception ex) {
            log.warn("RedisTemplate round-trip failed after startup - runtime Redis operations will fall back gracefully: {}", ex.getMessage());
        }

        Cache demandsCache = cacheManager.getCache("demands");
        if (demandsCache == null) {
            log.warn("CacheManager verification failed: 'demands' cache is not available");
            return;
        }

        try {
            String cacheKey = DIAGNOSTIC_KEY + ":cache";
            demandsCache.put(cacheKey, "ok");
            String cacheValue = Optional.ofNullable(demandsCache.get(cacheKey))
                    .map(Cache.ValueWrapper::get)
                    .map(Object::toString)
                    .orElse(null);
            demandsCache.evict(cacheKey);
            log.info("CacheManager round-trip successful for cache 'demands': {}", cacheValue);
        } catch (Exception ex) {
            log.warn("CacheManager round-trip failed after startup - cache layer will rely on runtime fallback handling: {}", ex.getMessage());
        }
    }
}
