package com.config;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.context.annotation.Bean;

import org.springframework.context.annotation.Configuration;

import org.springframework.web.cors.CorsConfiguration;

import org.springframework.web.cors.CorsConfigurationSource;

import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

import java.util.List;

@Configuration

public class CorsConfig {

    @Value("${cors.allowed-origins}")

    private String allowedOriginsRaw;

    @Bean

    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        List<String> allowedOrigins = Arrays.stream(allowedOriginsRaw.split(","))

                .map(String::trim)

                .filter(s -> !s.isEmpty())

                .toList();

        configuration.setAllowedOrigins(allowedOrigins);

        configuration.setAllowedMethods(List.of(

                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"

        ));

        configuration.setAllowedHeaders(List.of(

                "Authorization", "Content-Type", "Accept"

        ));

        configuration.setExposedHeaders(List.of("Authorization"));

        configuration.setAllowCredentials(true);

        configuration.setMaxAge(1728000L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;

    }

}
