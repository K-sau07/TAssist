package com.tassist.infrastructure.web.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tassist.domain.port.out.EmbeddingClient;
import com.tassist.domain.port.out.LLMClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** §12.4 chat endpoints (non-stream) over real Postgres. Fake LLM + embedder for offline runs. */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Import(ChatEndpointsTest.FakeAiConfig.class)
class ChatEndpointsTest {

    @TestConfiguration
    static class FakeAiConfig {
        @Bean @Primary EmbeddingClient fakeEmbed() {
            return new EmbeddingClient() {
                public float[] embed(String t) { float[] v = new float[1024]; v[0] = 1f; return v; }
                public List<float[]> embedBatch(List<String> ts) {
                    List<float[]> o = new ArrayList<>(); for (String t : ts) o.add(embed(t)); return o; }
                public int dimension() { return 1024; }
            };
        }
        @Bean @Primary LLMClient fakeLlm() {
            return new LLMClient() {
                public LlmResponse complete(LlmRequest r) {
                    return new LlmResponse("This is a grounded answer [S1].", 50, 12); }
                public void stream(LlmRequest r, StreamEvents e) {}
            };
        }
    }

    @Container @ServiceConnection
    static PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
        DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"));

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.flyway.enabled", () -> "true");
        r.add("tassist.jwt.secret", () -> "test_secret_at_least_32_bytes_long_hs256_ok");
        r.add("tassist.storage.dir", () -> System.getProperty("java.io.tmpdir") + "/tassist-chat-test");
        r.add("spring.data.redis.repositories.enabled", () -> "false");
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private String tokenFor(String email) throws Exception {
        String resp = mvc.perform(post("/api/auth/signup").contentType("application/json")
                .content("{\"email\":\"" + email + "\",\"displayName\":\"U\",\"password\":\"password123\"}"))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(resp).get("token").asText();
    }
    private String createChat(String token) throws Exception {
        String resp = mvc.perform(post("/api/chats").header("Authorization", "Bearer " + token)
                .contentType("application/json").content("{\"scope\":\"REGULAR\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.scope").value("REGULAR"))
            .andReturn().getResponse().getContentAsString();
        return json.readTree(resp).get("id").asText();
    }

    @Test void full_lifecycle_create_list_get_rename_message_delete() throws Exception {
        String token = tokenFor("chat_" + System.nanoTime() + "@ex.com");
        String chatId = createChat(token);

        // list -> 1
        mvc.perform(get("/api/chats").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));

        // send a message (non-stream) -> persists user + assistant
        mvc.perform(post("/api/chats/" + chatId + "/messages").header("Authorization", "Bearer " + token)
                .contentType("application/json").content("{\"content\":\"hello there\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.userMessage.role").value("USER"))
            .andExpect(jsonPath("$.assistantMessage.role").value("ASSISTANT"))
            .andExpect(jsonPath("$.mode").value("REGULAR"));

        // get chat with messages -> 2 messages
        mvc.perform(get("/api/chats/" + chatId).header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.messages.length()").value(2));

        // rename
        mvc.perform(patch("/api/chats/" + chatId).header("Authorization", "Bearer " + token)
                .contentType("application/json").content("{\"title\":\"Renamed\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Renamed"));

        // delete
        mvc.perform(delete("/api/chats/" + chatId).header("Authorization", "Bearer " + token))
            .andExpect(status().isNoContent());
        mvc.perform(get("/api/chats").header("Authorization", "Bearer " + token))
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test void cannot_access_another_users_chat() throws Exception {
        String a = tokenFor("owner_" + System.nanoTime() + "@ex.com");
        String b = tokenFor("intruder_" + System.nanoTime() + "@ex.com");
        String chatId = createChat(a);
        mvc.perform(get("/api/chats/" + chatId).header("Authorization", "Bearer " + b))
            .andExpect(status().isForbidden());
    }

    @Test void unauthenticated_is_401() throws Exception {
        mvc.perform(get("/api/chats")).andExpect(status().isUnauthorized());
    }

    @Test void folder_scope_without_folderId_is_422() throws Exception {
        String token = tokenFor("fld_" + System.nanoTime() + "@ex.com");
        mvc.perform(post("/api/chats").header("Authorization", "Bearer " + token)
                .contentType("application/json").content("{\"scope\":\"FOLDER\"}"))
            .andExpect(status().isUnprocessableEntity());
    }
}
