package com.controller;

import com.dto.centralised_dto.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/redis")
public class RedisHealthController {

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @GetMapping("/health")
    public ApiResponse<?> checkRedisHealth() {
        Map<String, Object> status = new HashMap<>();
        
        try {
            // Test Redis connection
            redisConnectionFactory.getConnection().ping();
            status.put("redis", "UP");
            status.put("status", "Connected");
            status.put("message", "Redis connection is working");
            status.put("cacheEnabled", true);
            return ApiResponse.success("Redis is healthy", status);
        } catch (Exception e) {
            status.put("redis", "DOWN");
            status.put("status", "Disconnected");
            status.put("error", e.getMessage());
            status.put("message", "Redis connection failed - application running in degraded mode");
            status.put("cacheEnabled", false);
            status.put("impact", "Application will continue without caching");
            return ApiResponse.success("Redis is down", status);
        }
    }
}
