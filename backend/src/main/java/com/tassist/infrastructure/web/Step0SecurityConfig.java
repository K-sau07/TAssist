package com.tassist.infrastructure.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * PLACEHOLDER security config for Step 0 scaffolding only.
 *
 * <p>Its sole job is to keep the empty app bootable and let the Step 0 acceptance
 * check reach {@code GET /api/health}. The real security configuration — stateless
 * JWT filter chain, OAuth2 client, and the §10 endpoint authorization matrix — is
 * built in Step 3 and REPLACES this class.
 */
@Configuration
public class Step0SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/health").permitAll()
                .anyRequest().permitAll()  // Step 0 only: real matrix arrives in Step 3.
            )
            .httpBasic(basic -> basic.disable())
            .formLogin(form -> form.disable());
        return http.build();
    }
}
