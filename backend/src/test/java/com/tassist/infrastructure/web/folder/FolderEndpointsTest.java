package com.tassist.infrastructure.web.folder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tassist.domain.port.out.EmbeddingClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
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

/** §12.3 folder endpoints over real Postgres. Offline fake embedder for file uploads. */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Import(FolderEndpointsTest.FakeEmbeddingConfig.class)
class FolderEndpointsTest {

    @TestConfiguration
    static class FakeEmbeddingConfig {
        @Bean @Primary
        EmbeddingClient fakeEmbeddingClient() {
            return new EmbeddingClient() {
                public float[] embed(String t) {
                    float[] v = new float[1024]; int h = t.hashCode();
                    for (int i = 0; i < v.length; i++) v[i] = ((h + i) % 100) / 100f; return v; }
                public List<float[]> embedBatch(List<String> ts) {
                    List<float[]> o = new ArrayList<>(); for (String t : ts) o.add(embed(t)); return o; }
                public int dimension() { return 1024; }
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
        r.add("tassist.storage.dir", () -> System.getProperty("java.io.tmpdir") + "/tassist-folder-test");
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private String tokenFor(String email) throws Exception {
        String resp = mvc.perform(post("/api/auth/signup").contentType("application/json")
                .content("{\"email\":\"" + email + "\",\"displayName\":\"U\",\"password\":\"password123\"}"))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(resp).get("token").asText();
    }

    private String createFolder(String token, String name) throws Exception {
        String resp = mvc.perform(post("/api/folders").header("Authorization", "Bearer " + token)
                .contentType("application/json").content("{\"name\":\"" + name + "\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value(name))
            .andReturn().getResponse().getContentAsString();
        return json.readTree(resp).get("id").asText();
    }

    private String uploadFile(String token, String name) throws Exception {
        var f = new MockMultipartFile("file", name, "text/plain", "hello".getBytes(StandardCharsets.UTF_8));
        String resp = mvc.perform(multipart("/api/files").file(f).header("Authorization", "Bearer " + token))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(resp).get("id").asText();
    }

    @Test
    void full_lifecycle_create_add_list_remove_delete() throws Exception {
        String token = tokenFor("fld_" + System.nanoTime() + "@ex.com");
        String folderId = createFolder(token, "Lectures");
        String fileId = uploadFile(token, "a.txt");

        // add file
        mvc.perform(post("/api/folders/" + folderId + "/files").header("Authorization", "Bearer " + token)
                .contentType("application/json").content("{\"fileIds\":[\"" + fileId + "\"]}"))
            .andExpect(status().isNoContent());

        // list files -> 1
        mvc.perform(get("/api/folders/" + folderId + "/files").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(fileId));

        // remove file
        mvc.perform(delete("/api/folders/" + folderId + "/files/" + fileId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isNoContent());
        mvc.perform(get("/api/folders/" + folderId + "/files").header("Authorization", "Bearer " + token))
            .andExpect(jsonPath("$.length()").value(0));

        // delete folder
        mvc.perform(delete("/api/folders/" + folderId).header("Authorization", "Bearer " + token))
            .andExpect(status().isNoContent());
        mvc.perform(get("/api/folders").header("Authorization", "Bearer " + token))
            .andExpect(jsonPath("$.length()").value(0));

        // file survives folder deletion (§8)
        mvc.perform(get("/api/files/" + fileId).header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());
    }

    @Test
    void rename_folder() throws Exception {
        String token = tokenFor("ren_" + System.nanoTime() + "@ex.com");
        String id = createFolder(token, "Old");
        mvc.perform(patch("/api/folders/" + id).header("Authorization", "Bearer " + token)
                .contentType("application/json").content("{\"name\":\"New\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("New"));
    }

    @Test
    void duplicate_name_is_409() throws Exception {
        String token = tokenFor("dup_" + System.nanoTime() + "@ex.com");
        createFolder(token, "Dup");
        mvc.perform(post("/api/folders").header("Authorization", "Bearer " + token)
                .contentType("application/json").content("{\"name\":\"Dup\"}"))
            .andExpect(status().isConflict());
    }

    @Test
    void cannot_touch_another_users_folder() throws Exception {
        String a = tokenFor("owner_" + System.nanoTime() + "@ex.com");
        String b = tokenFor("intruder_" + System.nanoTime() + "@ex.com");
        String folderId = createFolder(a, "Private");
        mvc.perform(delete("/api/folders/" + folderId).header("Authorization", "Bearer " + b))
            .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticated_is_401() throws Exception {
        mvc.perform(get("/api/folders")).andExpect(status().isUnauthorized());
    }
}
