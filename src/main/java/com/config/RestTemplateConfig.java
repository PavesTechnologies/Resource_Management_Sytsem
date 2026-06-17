package com.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Configuration
@Slf4j
public class RestTemplateConfig {

    @Value("${external.auth.url}")
    private String authUrl;

    @Value("${external.auth.email}")
    private String authEmail;

    @Value("${external.auth.password}")
    private String authPassword;

    @Value("${external.token.cache.minutes:5}")
    private long tokenCacheMinutes;

    @Value("${external.token.refresh.buffer.minutes:1}")
    private long refreshBufferMinutes;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // volatile for cross-thread visibility; synchronized on refreshToken() prevents duplicate fetches
    private volatile String cachedToken;
    private volatile LocalDateTime tokenFetchedAt;
    // actual expiry from JWT exp claim; null means fallback to tokenCacheMinutes
    private volatile LocalDateTime tokenExpiresAt;

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setInterceptors(List.of(bearerTokenInterceptor()));
        return restTemplate;
    }

    private ClientHttpRequestInterceptor bearerTokenInterceptor() {
        // separate plain RestTemplate — no interceptors — to avoid recursive interception on the auth call itself
        RestTemplate authClient = new RestTemplate();
        return (request, body, execution) -> {
            request.getHeaders().setBearerAuth(getToken(authClient));
            return execution.execute(request, body);
        };
    }

    private String getToken(RestTemplate authClient) {
        if (isTokenValid()) {
            return cachedToken;
        }
        return refreshToken(authClient);
    }

    private boolean isTokenValid() {
        if (cachedToken == null || tokenFetchedAt == null) {
            return false;
        }
        // prefer JWT exp claim so we cache for the token's full real lifetime
        // fall back to configured tokenCacheMinutes if exp could not be parsed
        LocalDateTime effectiveExpiry = (tokenExpiresAt != null)
                ? tokenExpiresAt.minusMinutes(refreshBufferMinutes)
                : tokenFetchedAt.plusMinutes(tokenCacheMinutes - refreshBufferMinutes);
        return LocalDateTime.now(ZoneOffset.UTC).isBefore(effectiveExpiry);
    }

    private synchronized String refreshToken(RestTemplate authClient) {
        if (isTokenValid()) {
            return cachedToken; // another thread already refreshed while we waited on the lock
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, String> requestBody = Map.of("email", authEmail, "password", authPassword);

            ResponseEntity<String> response = authClient.postForEntity(
                    authUrl, new HttpEntity<>(requestBody, headers), String.class);

            cachedToken = extractToken(response.getBody());
            tokenFetchedAt = LocalDateTime.now(ZoneOffset.UTC);
            log.info("External auth token refreshed successfully, expires at: {}", tokenExpiresAt);
            return cachedToken;
        } catch (Exception e) {
            log.error("Failed to fetch external auth token from {}: {}", authUrl, e.getMessage());
            throw new RuntimeException("Unable to obtain external auth token", e);
        }
    }

    private String extractToken(String responseBody) throws Exception {
        if (responseBody == null || responseBody.isBlank()) {
            throw new IllegalStateException("Empty response from auth endpoint");
        }
        JsonNode root = objectMapper.readTree(responseBody);
        String token = findTokenField(root);
        if (token == null) {
            // some auth APIs wrap the token inside a "data" envelope
            JsonNode data = root.get("data");
            if (data != null && !data.isNull()) {
                token = findTokenField(data);
            }
        }
        if (token == null) {
            throw new IllegalStateException("Token field not found in auth response");
        }
        tokenExpiresAt = parseJwtExpiry(token);
        return token;
    }

    // Decodes the JWT payload and reads the exp claim to get the real token expiry.
    // Returns null if the claim cannot be parsed — isTokenValid() falls back to tokenCacheMinutes.
    private LocalDateTime parseJwtExpiry(String jwt) {
        try {
            String[] parts = jwt.split("\\.");
            if (parts.length < 2) return null;
            String payload = parts[1];
            int mod = payload.length() % 4;
            if (mod != 0) payload += "=".repeat(4 - mod);
            String decoded = new String(Base64.getUrlDecoder().decode(payload));
            JsonNode jwtPayload = objectMapper.readTree(decoded);
            JsonNode expNode = jwtPayload.get("exp");
            if (expNode == null || expNode.isNull()) return null;
            return LocalDateTime.ofInstant(Instant.ofEpochSecond(expNode.asLong()), ZoneOffset.UTC);
        } catch (Exception e) {
            log.debug("Could not parse JWT exp claim, will use tokenCacheMinutes fallback");
            return null;
        }
    }

    private String findTokenField(JsonNode node) {
        for (String field : new String[]{"token", "accessToken", "access_token", "jwtToken", "jwt"}) {
            JsonNode candidate = node.get(field);
            if (candidate != null && !candidate.isNull() && !candidate.asText().isBlank()) {
                return candidate.asText();
            }
        }
        return null;
    }
}
