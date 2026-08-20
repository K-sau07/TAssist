package com.tassist.infrastructure.web.auth;

import com.tassist.domain.model.User;
import com.tassist.domain.port.in.AuthUseCase;
import com.tassist.domain.port.in.TokenUseCase;
import com.tassist.infrastructure.security.GoogleOAuthClient;
import com.tassist.infrastructure.security.GoogleOAuthClient.GoogleProfile;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

/**
 * Manual Google OAuth (D15). /authorize -> Google consent; /callback -> exchange code,
 * upsert user, mint our JWT, redirect to {frontendUrl}/auth/complete?token=... (§12.1).
 * CSRF-protected via a short-lived state cookie compared on callback.
 */
@RestController
public class GoogleAuthController {

    private static final String STATE_COOKIE = "g_oauth_state";

    private final GoogleOAuthClient google;
    private final AuthUseCase auth;
    private final TokenUseCase tokens;
    private final String frontendUrl;

    public GoogleAuthController(GoogleOAuthClient google, AuthUseCase auth, TokenUseCase tokens,
                               @Value("${tassist.frontend-url:http://localhost:5173}") String frontendUrl) {
        this.google = google;
        this.auth = auth;
        this.tokens = tokens;
        this.frontendUrl = frontendUrl;
    }

    @GetMapping("/api/auth/google/authorize")
    public ResponseEntity<Void> authorize(HttpServletResponse response) {
        if (!google.isConfigured()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        String state = UUID.randomUUID().toString();
        Cookie cookie = new Cookie(STATE_COOKIE, state);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(300); // 5 min
        response.addCookie(cookie);
        return ResponseEntity.status(HttpStatus.FOUND)
            .location(URI.create(google.authorizeUrl(state)))
            .build();
    }

    @GetMapping("/api/auth/google/callback")
    public ResponseEntity<Void> callback(@RequestParam("code") String code,
                                         @RequestParam(value = "state", required = false) String state,
                                         HttpServletRequest request,
                                         HttpServletResponse response) {
        validateState(state, request);
        clearStateCookie(response);

        GoogleProfile profile = google.exchangeCode(code);
        User user = auth.upsertGoogleUser(profile.subject(), profile.email(), profile.name());
        String jwt = tokens.issueToken(user.id());

        String target = UriComponentsBuilder.fromUriString(frontendUrl)
            .path("/auth/complete")
            .queryParam("token", jwt)
            .build().toUriString();
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(target)).build();
    }

    private void validateState(String state, HttpServletRequest request) {
        if (state == null || request.getCookies() == null) {
            throw new com.tassist.infrastructure.security.OAuthException("missing OAuth state");
        }
        String expected = null;
        for (Cookie c : request.getCookies()) {
            if (STATE_COOKIE.equals(c.getName())) { expected = c.getValue(); break; }
        }
        if (expected == null || !expected.equals(state)) {
            throw new com.tassist.infrastructure.security.OAuthException("OAuth state mismatch");
        }
    }

    private void clearStateCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(STATE_COOKIE, "");
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }
}
