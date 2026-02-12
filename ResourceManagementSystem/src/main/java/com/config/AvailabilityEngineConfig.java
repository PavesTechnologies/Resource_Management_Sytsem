package com.config;

import com.service_imple.external_api_impl.TokenService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AvailabilityEngineConfig {

    private final TokenService tokenService;

    public AvailabilityEngineConfig(@Lazy TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        
        // Add interceptor to use token from TokenService
        List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
        interceptors.add((request, body, execution) -> {
            // Add content type only if body is present
            if (body.length > 0 && !request.getHeaders().containsKey(HttpHeaders.CONTENT_TYPE)) {
                request.getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json");
            }
            
            // Get token from TokenService
            try {
                String token = tokenService.getAccessToken();
                if (token != null && !token.isEmpty()) {
                    request.getHeaders().add(HttpHeaders.AUTHORIZATION, "Bearer " + token);
                }
            } catch (Exception e) {
                // Continue without token if there's an issue
            }
            
            return execution.execute(request, body);
        });
        
        restTemplate.setInterceptors(interceptors);
        return restTemplate;
    }

    @Bean(name = "availabilityCalculationExecutor")
    public Executor availabilityCalculationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("AvailabilityCalc-");
        executor.initialize();
        return executor;
    }
}
