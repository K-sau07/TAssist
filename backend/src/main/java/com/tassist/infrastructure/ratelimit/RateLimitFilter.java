package com.tassist.infrastructure.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tassist.domain.vo.UserId;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * §16.1 short-window rate limiting via in-memory Bucket4j (D20). Runs after JWT auth so the userId
 * principal is available; keys buckets by userId (or client IP for per-IP rules). On breach: 429
 * RATE_LIMITED with a Retry-After header and the §16.1 body shape.
 */
@Component
@Order(50) // after Spring Security's filter chain populates the SecurityContext
public class RateLimitFilter extends OncePerRequestFilter {

    private final ObjectMapper json = new ObjectMapper();
    @Value("${tassist.ratelimit.enabled:true}") private boolean enabled;
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    // §16.1 table (first match wins; global authenticated rule last).
    private final List<RateLimitRule> rules = List.of(
        new RateLimitRule("login",   "POST", "/api/auth/login",  true,  10, Duration.ofSeconds(6)),
        new RateLimitRule("signup",  "POST", "/api/auth/signup", true,  5,  Duration.ofSeconds(60)),
        new RateLimitRule("upload",  "POST", "/api/files",       false, 20, Duration.ofSeconds(60)),
        new RateLimitRule("stream",  "POST", "/api/.*/messages/stream", false, 30, Duration.ofSeconds(10)),
        new RateLimitRule("join",    "POST", "/api/channels/[^/]+/join", false, 5, Duration.ofSeconds(300)),
        new RateLimitRule("global",  "*",    "/api/.*",          false, 200, Duration.ofSeconds(1)));

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!enabled) return true;
        String p = request.getRequestURI();
        return p.equals("/api/health") || p.startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        String method = request.getMethod();

        RateLimitRule rule = null;
        for (RateLimitRule r : rules) {
            if (("*".equals(r.method()) || r.method().equalsIgnoreCase(method)) && path.matches(r.pathRegex())) {
                rule = r; break;
            }
        }
        if (rule == null) { chain.doFilter(request, response); return; }
        final RateLimitRule matched = rule;

        String principalKey = resolveKey(request, matched);
        String bucketKey = matched.name() + ":" + principalKey;
        Bucket bucket = buckets.computeIfAbsent(bucketKey, k -> Bucket.builder().addLimit(matched.bandwidth()).build());

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            chain.doFilter(request, response);
        } else {
            long retryAfter = Math.max(1, Duration.ofNanos(probe.getNanosToWaitForRefill()).toSeconds());
            write429(response, retryAfter);
        }
    }

    private String resolveKey(HttpServletRequest request, RateLimitRule rule) {
        if (!rule.perIp()) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof UserId uid) return "u:" + uid.value();
        }
        return "ip:" + clientIp(request);
    }

    private String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return request.getRemoteAddr();
    }

    private void write429(HttpServletResponse response, long retryAfter) throws IOException {
        response.setStatus(429);
        response.setContentType("application/json");
        response.setHeader("Retry-After", String.valueOf(retryAfter));
        Map<String, Object> error = Map.of(
            "code", "RATE_LIMITED",
            "message", "Too many requests. Try again in " + retryAfter + " seconds.",
            "retryAfterSeconds", retryAfter,
            "correlationId", UUID.randomUUID().toString());
        response.getWriter().write(json.writeValueAsString(Map.of("error", error)));
    }
}
