package com.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalDateTime;

@Service
public class TokenService {

    @Value("${external.auth.url}")
    private String authUrl;

    @Value("${external.auth.email}")
    private String email;

    @Value("${external.auth.password}")
    private String password;

    private String cachedToken;
    private LocalDateTime tokenExpiry;
    private final RestTemplate restTemplate;

    public TokenService() {
        this.restTemplate = new RestTemplate(); // Simple RestTemplate without interceptors
    }

    public String getAccessToken() {
        synchronized (this) {
            // Return cached token if still valid (cache for 50 minutes)
            if (cachedToken != null && tokenExpiry != null && tokenExpiry.isAfter(LocalDateTime.now().plusMinutes(10))) {
                return cachedToken;
            }

            // Get new token
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                Map<String, String> credentials = Map.of(
                    "email", email,
                    "password", password
                );

                HttpEntity<Map<String, String>> request = new HttpEntity<>(credentials, headers);
                
                ResponseEntity<Map> response = restTemplate.exchange(
                    authUrl,
                    HttpMethod.POST,
                    request,
                    Map.class
                );

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    Map<String, Object> responseBody = response.getBody();
                    String accessToken = (String) responseBody.get("access_token");
                    
                    if (accessToken != null) {
                        cachedToken = accessToken;
                        tokenExpiry = LocalDateTime.now().plusMinutes(50); // Cache for 50 minutes
                        return accessToken;
                    }
                }
            } catch (Exception e) {
                // Log error but don't throw - allow system to continue without token
                System.err.println("Failed to get access token: " + e.getMessage());
            }

            return null;
        }
    }

    public void invalidateToken() {
        synchronized (this) {
            cachedToken = null;
            tokenExpiry = null;
        }
    }
}
