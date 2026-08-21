package com.tassist.application.retrieval;

import com.tassist.domain.model.*;
import com.tassist.domain.port.in.RetrievalUseCase;
import com.tassist.domain.port.in.RetrievalUseCase.*;
import com.tassist.domain.port.out.*;
import com.tassist.domain.vo.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * §20 Step 8 acceptance: call RetrievalService.retrieve() against real Postgres/pgvector
 * and assert chunks are correctly scoped by folder and by @mentions.
 * Uses a controlled fake embedder so cosine distances are deterministic.
 */
@SpringBootTest
@Testcontainers
@Import(RetrievalIntegrationTest.ControlledEmbeddingConfig.class)
class RetrievalIntegrationTest {

    @Container @ServiceConnection
    static PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
        DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"));

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.flyway.enabled", () -> "true");
        r.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        r.add("spring.data.redis.repositories.enabled", () -> "false");
    }

    /**
     * Deterministic embedder: the query "APAC sales" and text "APAC sales revenue" both map to
     * axis 0; unrelated text maps to axis 1. Cosine similarity is ~1 for same-axis, ~0 otherwise,
     * so relevance is fully controlled.
     */
    @TestConfiguration
    static class ControlledEmbeddingConfig {
        @Bean @Primary
        EmbeddingClient controlledEmbedder() {
            return new EmbeddingClient() {
                public float[] embed(String text) {
                    float[] v = new float[1024];
                    v[text.toLowerCase().contains("apac") ? 0 : 1] = 1f;
                    return v;
                }
                public List<float[]> embedBatch(List<String> texts) {
                    return texts.stream().map(this::embed).toList();
                }
                public int dimension() { return 1024; }
            };
        }
    }

    @Autowired RetrievalUseCase retrieval;
    @Autowired EmbeddingClient embedder;
    @Autowired UserRepository users;
    @Autowired FileRepository files;
    @Autowired ChunkRepository chunks;
    @Autowired FolderRepository folders;
    @Autowired FolderFileRepository folderFiles;

    private User user;

    private User seedUser() {
        Instant now = Instant.now();
        return users.save(new User(UserId.newId(), "ret_" + System.nanoTime() + "@ex.com",
            "R", Optional.of("$2a$h"), AuthProvider.PASSWORD, Optional.empty(), now, now));
    }

    private FileId seedFileWithChunk(UserId owner, String filename, String chunkText) {
        Instant now = Instant.now();
        FileId fid = FileId.newId();
        files.save(new File(fid, owner, filename, FileType.PDF, 1, "k/" + fid.value(),
            "h" + fid.value(), FileStatus.READY, Optional.empty(), now, now));
        chunks.saveAll(List.of(new Chunk(ChunkId.newId(), fid, 0, chunkText,
            Map.of("page", "1"), embedder.embed(chunkText))));
        return fid;
    }

    @Test
    void folder_scope_returns_only_folder_files_ranked_by_relevance() {
        user = seedUser();
        FileId relevant = seedFileWithChunk(user.id(), "sales.pdf", "APAC sales revenue by quarter");
        FileId offtopic = seedFileWithChunk(user.id(), "hr.pdf", "employee vacation policy");
        Folder folder = folders.save(new Folder(FolderId.newId(), user.id(), "Work", Instant.now()));
        folderFiles.add(new FolderFile(folder.id(), relevant, Instant.now()));
        folderFiles.add(new FolderFile(folder.id(), offtopic, Instant.now()));

        RetrievalResult r = retrieval.retrieve(new RetrievalQuery(
            user.id(), "APAC sales", Scope.FOLDER, Optional.of(folder.id()), Optional.empty(), List.of()));

        // Only the relevant chunk passes the 0.4 floor; offtopic (~0 cosine) is dropped.
        assertThat(r.textHits()).hasSize(1);
        assertThat(r.textHits().get(0).chunk().fileId()).isEqualTo(relevant);
        assertThat(r.textHits().get(0).similarity()).isGreaterThan(0.9);
    }

    @Test
    void folder_scope_excludes_files_not_in_folder() {
        user = seedUser();
        FileId inFolder = seedFileWithChunk(user.id(), "a.pdf", "APAC sales in region");
        FileId outsideFolder = seedFileWithChunk(user.id(), "b.pdf", "APAC sales elsewhere");
        Folder folder = folders.save(new Folder(FolderId.newId(), user.id(), "Scoped", Instant.now()));
        folderFiles.add(new FolderFile(folder.id(), inFolder, Instant.now()));
        // outsideFolder deliberately NOT added

        RetrievalResult r = retrieval.retrieve(new RetrievalQuery(
            user.id(), "APAC sales", Scope.FOLDER, Optional.of(folder.id()), Optional.empty(), List.of()));

        assertThat(r.textHits()).extracting(h -> h.chunk().fileId())
            .containsExactly(inFolder)
            .doesNotContain(outsideFolder);
    }

    @Test
    void mentions_scope_targets_exactly_mentioned_files() {
        user = seedUser();
        FileId mentioned = seedFileWithChunk(user.id(), "target.pdf", "APAC sales detail");
        FileId other = seedFileWithChunk(user.id(), "other.pdf", "APAC sales other file");

        RetrievalResult r = retrieval.retrieve(new RetrievalQuery(
            user.id(), "APAC sales", Scope.MENTIONS, Optional.empty(), Optional.empty(), List.of(mentioned)));

        assertThat(r.textHits()).extracting(h -> h.chunk().fileId())
            .containsExactly(mentioned)
            .doesNotContain(other);
    }

    @Test
    void regular_scope_retrieves_nothing() {
        user = seedUser();
        seedFileWithChunk(user.id(), "x.pdf", "APAC sales content");
        RetrievalResult r = retrieval.retrieve(new RetrievalQuery(
            user.id(), "APAC sales", Scope.REGULAR, Optional.empty(), Optional.empty(), List.of()));
        assertThat(r.textHits()).isEmpty();
    }

    @Test
    void mention_of_another_users_file_is_dropped_with_warning() {
        user = seedUser();
        User stranger = seedUser();
        FileId theirs = seedFileWithChunk(stranger.id(), "private.pdf", "APAC sales secret");
        RetrievalResult r = retrieval.retrieve(new RetrievalQuery(
            user.id(), "APAC sales", Scope.MENTIONS, Optional.empty(), Optional.empty(), List.of(theirs)));
        assertThat(r.textHits()).isEmpty();
        assertThat(r.warnings()).hasSize(1);
    }
}
