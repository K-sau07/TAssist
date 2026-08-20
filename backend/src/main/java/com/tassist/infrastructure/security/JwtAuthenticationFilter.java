package com.tassist.infrastructure.security;

import com.tassist.domain.port.in.TokenUseCase;
import com.tassist.domain.vo.UserId;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Reads a bearer token from the Authorization header or the ?access_token= query param
 * (SSE fallback, §10), verifies it, and sets the SecurityContext with the userId as principal.
 * Never rejects here — unauthenticated requests simply proceed with no auth; the security
 * chain's authorization rules decide what needs a principal.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenUseCase tokens;

    public JwtAuthenticationFilter(TokenUseCase tokens) { this.tokens = tokens; }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain)
            throws ServletException, IOException {
        String token = extractToken(request);
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserId userId = tokens.verifyToken(token);
                var auth = new UsernamePasswordAuthenticationToken(
                    userId, null, AuthorityUtils.NO_AUTHORITIES);
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (RuntimeException ignored) {
                // Invalid token: leave context empty; protected routes will 401.
            }
        }
        chain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        String param = request.getParameter("access_token");
        return (param != null && !param.isBlank()) ? param : null;
    }
}
