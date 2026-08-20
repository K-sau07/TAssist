package com.tassist.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Binds tassist.jwt.* (secret + ttl). Secret must be >= 32 bytes (§10). */
@ConfigurationProperties(prefix = "tassist.jwt")
public class JwtProperties {
    private String secret;
    private int ttlHours = 24;

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
    public int getTtlHours() { return ttlHours; }
    public void setTtlHours(int ttlHours) { this.ttlHours = ttlHours; }
}
