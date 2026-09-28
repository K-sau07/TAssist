package com.tassist.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * CORS for the browser client. The UI is served from Vercel while the API runs
 * on Render, so every request is cross-origin.
 *
 * <p>Origins come from tassist.cors.allowed-origins so the deployed frontend URL
 * is configuration rather than code. Credentials are not enabled: authentication
 * is a stateless JWT sent in the Authorization header, not a cookie.
 */
@Configuration
public class CorsConfig {

    private final List<String> allowedOrigins;

    public CorsConfig(@Value("${tassist.cors.allowed-origins:http://localhost:5173}") String origins) {
        this.allowedOrigins = List.of(origins.split("\\s*,\\s*"));
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
