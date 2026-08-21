package com.tassist.infrastructure.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Refill;

import java.time.Duration;

/** A §16.1 rate-limit rule: which requests it matches + the bucket bandwidth. */
public record RateLimitRule(String name, String method, String pathRegex,
                            boolean perIp, int capacity, Duration refillPeriod) {

    public boolean matches(String reqMethod, String path) {
        return method.equalsIgnoreCase(reqMethod) && path.matches(pathRegex);
    }

    /** One token refilled every {@code refillPeriod}, capped at {@code capacity} (§16.1 "1 per Ns"). */
    public Bandwidth bandwidth() {
        return Bandwidth.classic(capacity, Refill.intervally(1, refillPeriod));
    }
}
