package com.tassist.infrastructure.web.auth;

import com.tassist.domain.error.Unauthenticated;
import com.tassist.domain.model.User;
import com.tassist.domain.port.in.AuthUseCase;
import com.tassist.domain.port.in.TokenUseCase;
import com.tassist.domain.vo.UserId;
import com.tassist.infrastructure.security.TokenService;
import com.tassist.infrastructure.web.auth.AuthDtos.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** §12.1 auth endpoints. Public routes under /api/auth/**; /api/me requires JWT. */
@RestController
public class AuthController {

    private final AuthUseCase auth;
    private final TokenUseCase tokens;
    private final TokenService tokenService; // for expiry timestamp

    public AuthController(AuthUseCase auth, TokenUseCase tokens, TokenService tokenService) {
        this.auth = auth;
        this.tokens = tokens;
        this.tokenService = tokenService;
    }

    @PostMapping("/api/auth/signup")
    public ResponseEntity<AuthResponse> signup(@RequestBody SignupRequest req) {
        User user = auth.signupWithPassword(req.email(), req.displayName(), req.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(authResponse(user));
    }

    @PostMapping("/api/auth/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest req) {
        User user = auth.loginWithPassword(req.email(), req.password());
        return ResponseEntity.ok(authResponse(user));
    }

    @PostMapping("/api/auth/logout")
    public ResponseEntity<Void> logout() {
        // Stateless JWT: client clears token. Server-side event logging is a later concern.
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/me")
    public ResponseEntity<UserView> me(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserId userId)) {
            throw new Unauthenticated("authentication required");
        }
        return ResponseEntity.ok(UserView.of(auth.getById(userId)));
    }

    private AuthResponse authResponse(User user) {
        String token = tokens.issueToken(user.id());
        return new AuthResponse(UserView.of(user), token, tokenService.expiryFromNow().toString());
    }
}
