package com.tassist.infrastructure.security;

import com.tassist.domain.error.Unauthenticated;
import com.tassist.domain.model.User;
import com.tassist.domain.port.in.TokenUseCase;
import com.tassist.domain.port.out.UserRepository;
import com.tassist.domain.vo.UserId;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

/** HS256 JWT mint/verify (§10). Implements the domain TokenUseCase port. */
@Service
public class TokenService implements TokenUseCase {

    private final SecretKey key;
    private final int ttlHours;
    private final UserRepository users;

    public TokenService(JwtProperties props, UserRepository users) {
        byte[] secret = props.getSecret() == null ? new byte[0]
            : props.getSecret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32)
            throw new IllegalStateException("tassist.jwt.secret must be >= 32 bytes (see §10)");
        this.key = Keys.hmacShaKeyFor(secret);
        this.ttlHours = props.getTtlHours();
        this.users = users;
    }

    @Override
    public String issueToken(UserId userId) {
        User u = users.findById(userId)
            .orElseThrow(() -> new Unauthenticated("user not found for token issue"));
        Instant now = Instant.now();
        Instant exp = now.plus(ttlHours, ChronoUnit.HOURS);
        return Jwts.builder()
            .subject(userId.value().toString())
            .claim("email", u.email())
            .issuedAt(Date.from(now))
            .expiration(Date.from(exp))
            .id(UUID.randomUUID().toString())
            .signWith(key)
            .compact();
    }

    @Override
    public UserId verifyToken(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload();
            return UserId.of(claims.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            throw new Unauthenticated("invalid or expired token");
        }
    }

    /** Seconds until expiry, for building expiresAt in responses. */
    public Instant expiryFromNow() {
        return Instant.now().plus(ttlHours, ChronoUnit.HOURS);
    }
}
