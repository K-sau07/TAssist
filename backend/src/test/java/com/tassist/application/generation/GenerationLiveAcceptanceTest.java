package com.tassist.application.generation;

import com.tassist.domain.model.*;
import com.tassist.domain.port.in.RetrievalUseCase;
import com.tassist.domain.port.in.RetrievalUseCase.*;
import com.tassist.domain.port.out.*;
import com.tassist.domain.vo.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
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
 * §20 Step 9 LIVE acceptance: ingest a fixture doc, ask a question, get a grounded answer
 * with [S1] markers from a REAL Claude call. Requires ANTHROPIC_API_KEY + VOYAGE_API_KEY in env
 * (run with `.env` sourced). Skipped automatically when keys are absent so the offline suite is unaffected.
 */
@SpringBootTest
@Testcontainers
@EnabledIfEnvironmentVariable(named = "ANTHROPIC_API_KEY", matches = ".+")
class GenerationLiveAcceptanceTest {

    @Container @ServiceConnection
    static PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
        DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"));

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.flyway.enabled", () -> "true");
        r.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        r.add("spring.data.redis.repositories.enabled", () -> "false");
    }

    @Autowired RetrievalUseCase retrieval;
    @Autowired GenerationService generation;
    @Autowired EmbeddingClient embeddings;
    @Autowired UserRepository users;
    @Autowired FileRepository files;
    @Autowired ChunkRepository chunks;

    @Test
    void ingest_fixture_ask_question_get_grounded_answer_with_citation() {
        Instant now = Instant.now();
        User user = users.save(new User(UserId.newId(), "live_" + System.nanoTime() + "@ex.com",
            "Live", Optional.of("$2a$h"), AuthProvider.PASSWORD, Optional.empty(), now, now));

        // Fixture "document": a specific, checkable fact the model can only get from the excerpt.
        FileId fid = FileId.newId();
        files.save(new File(fid, user.id(), "policy.pdf", FileType.PDF, 1, "k/" + fid.value(),
            "h" + fid.value(), FileStatus.READY, Optional.empty(), now, now));
        String fact = "The TAssist annual leave policy grants every full-time employee "
            + "exactly 27 days of paid vacation per calendar year, plus 3 floating holidays.";
        chunks.saveAll(List.of(new Chunk(ChunkId.newId(), fid, 0, fact,
            Map.of("page", "1"), embeddings.embed(fact))));

        // Retrieve (MENTIONS scope targeting the fixture), then generate grounded.
        RetrievalResult r = retrieval.retrieve(new RetrievalQuery(
            user.id(), "How many paid vacation days do full-time employees get?",
            Scope.MENTIONS, Optional.empty(), Optional.empty(), List.of(fid)));
        assertThat(r.textHits()).as("fixture chunk should be retrieved").isNotEmpty();

        var outcome = generation.generate(
            "How many paid vacation days do full-time employees get?", r, false);

        System.out.println("=== LIVE ANSWER ===\n" + outcome.answer()
            + "\n=== mode=" + outcome.mode() + " in=" + outcome.inputTokens()
            + " out=" + outcome.outputTokens() + " ===");

        assertThat(outcome.mode()).isEqualTo(GenerationService.Mode.GROUNDED);
        assertThat(outcome.answer()).contains("[S1]");        // §20: grounded answer with [S1] markers
        assertThat(outcome.answer()).contains("27");          // the fact came from the excerpt
        assertThat(outcome.outputTokens()).isGreaterThan(0);
    }
}
