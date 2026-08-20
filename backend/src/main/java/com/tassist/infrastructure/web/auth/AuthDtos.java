package com.tassist.infrastructure.web.auth;

import com.tassist.domain.model.User;
import java.time.Instant;

/** Request/response shapes for §12.1 auth endpoints. IDs returned as strings (§12.9). */
public final class AuthDtos {
    private AuthDtos() {}

    public record SignupRequest(String email, String displayName, String password) {}
    public record LoginRequest(String email, String password) {}

    public record UserView(String id, String email, String displayName,
                           String authProvider, String createdAt) {
        public static UserView of(User u) {
            return new UserView(
                u.id().value().toString(),
                u.email(),
                u.displayName(),
                u.authProvider().name(),
                u.createdAt().toString()
            );
        }
    }

    public record AuthResponse(UserView user, String token, String expiresAt) {}
}
