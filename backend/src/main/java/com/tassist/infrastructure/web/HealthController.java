package com.tassist.infrastructure.web;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Liveness endpoint (spec §12.8). Readiness ({@code /api/health/deep}) is added in a
 * later step once Postgres/Redis/Anthropic clients exist.
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public Map<String, Boolean> health() {
        return Map.of("ok", true);
    }
}
