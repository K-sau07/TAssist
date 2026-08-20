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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/** Full HTTP auth flow (controller + security + JWT + error envelopes) over real Postgres. */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AuthEndpointsTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
        DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"));

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.flyway.enabled", () -> "true");
        r.add("tassist.jwt.secret", () -> "test_secret_at_least_32_bytes_long_hs256_ok");
        r.add("tassist.jwt.ttl-hours", () -> "24");
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private String body(String email, String name, String pw) {
        return "{\"email\":\"" + email + "\",\"displayName\":\"" + name + "\",\"password\":\"" + pw + "\"}";
    }

    @Test
    void full_flow_signup_login_me() throws Exception {
        String email = "flow" + System.nanoTime() + "@example.com";

        // signup -> 201, returns token
        String signupResp = mvc.perform(post("/api/auth/signup")
                .contentType("application/json").content(body(email, "Flow", "password123")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.user.email").value(email))
            .andExpect(jsonPath("$.user.authProvider").value("PASSWORD"))
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andReturn().getResponse().getContentAsString();
        String token = json.readTree(signupResp).get("token").asText();

        // login -> 200
        mvc.perform(post("/api/auth/login")
                .contentType("application/json")
                .content("{\"email\":\"" + email + "\",\"password\":\"password123\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty());

        // /api/me with token -> 200
        mvc.perform(get("/api/me").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void me_without_token_is_401_unauthenticated() throws Exception {
        mvc.perform(get("/api/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
    }

    @Test
    void duplicate_email_is_409() throws Exception {
        String email = "dup" + System.nanoTime() + "@example.com";
        mvc.perform(post("/api/auth/signup").contentType("application/json").content(body(email, "A", "password123")))
            .andExpect(status().isCreated());
        mvc.perform(post("/api/auth/signup").contentType("application/json").content(body(email, "B", "password123")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error.code").value("EMAIL_TAKEN"));
    }

    @Test
    void bad_password_is_422_with_details() throws Exception {
        mvc.perform(post("/api/auth/signup").contentType("application/json")
                .content(body("v" + System.nanoTime() + "@example.com", "A", "short")))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.error.details.password").exists());
    }

    @Test
    void wrong_password_login_is_401_invalid_credentials() throws Exception {
        String email = "wp" + System.nanoTime() + "@example.com";
        mvc.perform(post("/api/auth/signup").contentType("application/json").content(body(email, "A", "password123")))
            .andExpect(status().isCreated());
        mvc.perform(post("/api/auth/login").contentType("application/json")
                .content("{\"email\":\"" + email + "\",\"password\":\"wrongpass99\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"));
    }
}
