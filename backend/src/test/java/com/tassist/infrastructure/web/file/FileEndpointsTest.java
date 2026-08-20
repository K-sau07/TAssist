package com.tassist.infrastructure.web.file;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tassist.domain.port.out.EmbeddingClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** File upload/list/status/delete over real Postgres + local-disk storage.
 *  Uses a deterministic fake EmbeddingClient so ingestion runs offline (no VOYAGE_API_KEY). */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@org.springframework.context.annotation.Import(FileEndpointsTest.FakeEmbeddingConfig.class)
class FileEndpointsTest {

    /** Deterministic 1024-dim embedder for tests — no network, matches VECTOR(1024). */
    @TestConfiguration
    static class FakeEmbeddingConfig {
        @Bean @Primary
        EmbeddingClient fakeEmbeddingClient() {
            return new EmbeddingClient() {
                @Override public float[] embed(String text) {
                    float[] v = new float[1024];
                    int h = text.hashCode();
                    for (int i = 0; i < v.length; i++) v[i] = ((h + i) % 100) / 100f;
                    return v;
                }
                @Override public List<float[]> embedBatch(List<String> texts) {
                    List<float[]> out = new ArrayList<>(texts.size());
                    for (String t : texts) out.add(embed(t));
                    return out;
                }
                @Override public int dimension() { return 1024; }
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
        r.add("tassist.storage.dir", () -> System.getProperty("java.io.tmpdir") + "/tassist-test-storage");
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private String tokenFor(String email) throws Exception {
        String resp = mvc.perform(post("/api/auth/signup").contentType("application/json")
                .content("{\"email\":\"" + email + "\",\"displayName\":\"U\",\"password\":\"password123\"}"))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(resp).get("token").asText();
    }

    private MockMultipartFile txt(String name, String body) {
        return new MockMultipartFile("file", name, "text/plain", body.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void upload_text_file_completes_to_ready_with_chunks() throws Exception {
        String token = tokenFor("f1_" + System.nanoTime() + "@example.com");
        String resp = mvc.perform(multipart("/api/files").file(txt("a.txt", "hello content"))
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.type").value("TXT"))
            .andExpect(jsonPath("$.status").value("READY"))
            .andReturn().getResponse().getContentAsString();
        String id = json.readTree(resp).get("id").asText();

        mvc.perform(get("/api/files/" + id + "/status").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("READY"));
    }

    @Test
    void duplicate_upload_returns_same_file() throws Exception {
        String token = tokenFor("f2_" + System.nanoTime() + "@example.com");
        var file = txt("dup.txt", "identical bytes here");
        String r1 = mvc.perform(multipart("/api/files").file(file).header("Authorization", "Bearer " + token))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String r2 = mvc.perform(multipart("/api/files").file(file).header("Authorization", "Bearer " + token))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat(json.readTree(r1).get("id").asText())
            .isEqualTo(json.readTree(r2).get("id").asText());
    }

    @Test
    void unsupported_type_is_415() throws Exception {
        String token = tokenFor("f3_" + System.nanoTime() + "@example.com");
        var bad = new MockMultipartFile("file", "data.json", "application/json", "{}".getBytes());
        mvc.perform(multipart("/api/files").file(bad).header("Authorization", "Bearer " + token))
            .andExpect(status().isUnsupportedMediaType())
            .andExpect(jsonPath("$.error.code").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    @Test
    void list_returns_uploaded_files() throws Exception {
        String token = tokenFor("f4_" + System.nanoTime() + "@example.com");
        mvc.perform(multipart("/api/files").file(txt("one.txt", "one")).header("Authorization", "Bearer " + token))
            .andExpect(status().isCreated());
        mvc.perform(get("/api/files").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void delete_removes_file() throws Exception {
        String token = tokenFor("f5_" + System.nanoTime() + "@example.com");
        String resp = mvc.perform(multipart("/api/files").file(txt("del.txt", "delete me"))
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id = json.readTree(resp).get("id").asText();
        mvc.perform(delete("/api/files/" + id).header("Authorization", "Bearer " + token))
            .andExpect(status().isNoContent());
        mvc.perform(get("/api/files/" + id).header("Authorization", "Bearer " + token))
            .andExpect(status().isNotFound());
    }

    @Test
    void upload_without_auth_is_401() throws Exception {
        mvc.perform(multipart("/api/files").file(txt("x.txt", "x")))
            .andExpect(status().isUnauthorized());
    }
}
