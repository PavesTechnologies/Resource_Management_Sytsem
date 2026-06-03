package com.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;

import java.io.IOException;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplateBuilder restTemplateBuilder() {
        return new RestTemplateBuilder();
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .additionalInterceptors(new ClientHttpRequestInterceptor() {
                    @Override
                    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
                        // In a real application, you would obtain the JWT token dynamically
                        // For demonstration, we're using a placeholder.
                        // This token would typically be acquired by authenticating with the external API
                        // using specific credentials (e.g., client_id, client_secret, username, password).
                        String jwtToken = "YOUR_EXTERNAL_API_JWT_TOKEN"; // Replace with actual token acquisition logic

                        if (!jwtToken.isEmpty() && !request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                            request.getHeaders().add(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken);
                        }
                        return execution.execute(request, body);
                    }
                })
                .build();
    }
}
