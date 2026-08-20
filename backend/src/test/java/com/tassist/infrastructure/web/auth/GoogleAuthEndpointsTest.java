package com.tassist.infrastructure.web.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/** Google OAuth endpoints with dummy creds: authorize redirect + state CSRF guard.
 *  The code->token exchange (callback happy path) needs a real Google code and is not covered here. */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class GoogleAuthEndpointsTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
        DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"));

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.flyway.enabled", () -> "true");
        r.add("tassist.jwt.secret", () -> "test_secret_at_least_32_bytes_long_hs256_ok");
        r.add("tassist.google.client-id", () -> "test-client.apps.googleusercontent.com");
        r.add("tassist.google.client-secret", () -> "test-secret");
        r.add("tassist.google.redirect-uri", () -> "http://localhost:8080/api/auth/google/callback");
        r.add("tassist.frontend-url", () -> "http://localhost:5173");
    }

    @Autowired MockMvc mvc;

    @Test
    void authorize_redirects_to_google_consent() throws Exception {
        mvc.perform(get("/api/auth/google/authorize"))
            .andExpect(status().isFound())
            .andExpect(header().string("Location", containsString("accounts.google.com/o/oauth2/v2/auth")))
            .andExpect(header().string("Location", containsString("client_id=test-client")))
            .andExpect(header().string("Location", containsString("scope=openid")))
            .andExpect(header().string("Location", containsString("state=")))
            .andExpect(cookie().exists("g_oauth_state"))
            .andExpect(cookie().httpOnly("g_oauth_state", true));
    }

    @Test
    void callback_with_state_mismatch_is_401() throws Exception {
        mvc.perform(get("/api/auth/google/callback").param("code", "fake").param("state", "wrong")
                .cookie(new jakarta.servlet.http.Cookie("g_oauth_state", "different")))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
    }

    @Test
    void callback_without_state_cookie_is_401() throws Exception {
        mvc.perform(get("/api/auth/google/callback").param("code", "fake").param("state", "x"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
    }
}
