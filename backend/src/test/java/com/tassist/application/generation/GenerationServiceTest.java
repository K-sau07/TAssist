package com.tassist.application.generation;

import com.tassist.domain.model.*;
import com.tassist.domain.port.in.RetrievalUseCase.RetrievalResult;
import com.tassist.domain.port.in.RetrievalUseCase.TextHit;
import com.tassist.domain.port.out.FileRepository;
import com.tassist.domain.port.out.LLMClient;
import com.tassist.domain.vo.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/** Unit tests for §11.5/§11.6 non-streaming mode selection + sentinel fallback. */
class GenerationServiceTest {

    // fake LLM: returns a scripted response and records the last request's system prompt
    static class FakeLlm implements LLMClient {
        String reply = "answer [S1]";
        int in = 10, out = 5;
        String lastSystem;
        int calls = 0;
        public LlmResponse complete(LlmRequest r) {
            lastSystem = r.system(); calls++;
            return new LlmResponse(reply, in, out);
        }
        public void stream(LlmRequest r, StreamEvents e) {}
    }
    static class FakeFiles implements FileRepository {
        final Map<UUID, File> byId = new HashMap<>();
        public File save(File f) { byId.put(f.id().value(), f); return f; }
        public Optional<File> findById(FileId id) { return Optional.ofNullable(byId.get(id.value())); }
        public List<File> findByOwner(UserId o) { return List.of(); }
        public Optional<File> findByOwnerAndContentHash(UserId o, String h) { return Optional.empty(); }
        public List<File> findByOwnerAndFilename(UserId o, String n) { return List.of(); }
        public void delete(FileId id) {}
    }

    private final FakeLlm llm = new FakeLlm();
    private final FakeFiles files = new FakeFiles();
    private final GenerationService svc = new GenerationService(new PromptBuilder(), llm, files);

    private TextHit hit(String text) {
        FileId fid = FileId.newId();
        files.save(new File(fid, UserId.newId(), "doc.pdf", FileType.PDF, 1, "k", "h" + fid.value(),
            FileStatus.READY, Optional.empty(), Instant.now(), Instant.now()));
        return new TextHit(new Chunk(ChunkId.newId(), fid, 0, text, Map.of("page", "1"), new float[]{1f}), 0.9);
    }
    private RetrievalResult result(List<TextHit> hits) {
        return new RetrievalResult(hits, List.of(), false, List.of());
    }

    @Test void regular_scope_uses_regular_prompt() {
        var o = svc.generate("hi", null, true);
        assertThat(o.mode()).isEqualTo(GenerationService.Mode.REGULAR);
        assertThat(llm.lastSystem).contains("helpful, concise assistant");
    }

    @Test void hits_use_grounded_prompt_with_labels() {
        var o = svc.generate("q", result(List.of(hit("recursion text"))), false);
        assertThat(o.mode()).isEqualTo(GenerationService.Mode.GROUNDED);
        assertThat(llm.lastSystem).contains("[S1] (doc.pdf, page 1) recursion text");
    }

    @Test void no_hits_uses_fallback() {
        var o = svc.generate("q", result(List.of()), false);
        assertThat(o.mode()).isEqualTo(GenerationService.Mode.FALLBACK);
        assertThat(llm.lastSystem).contains("no relevant material was found");
    }

    @Test void sentinel_triggers_fallback_rerun_and_sums_tokens() {
        llm.reply = PromptBuilder.INSUFFICIENT_SENTINEL;
        var o = svc.generate("q", result(List.of(hit("weak text"))), false);
        assertThat(o.mode()).isEqualTo(GenerationService.Mode.FALLBACK);
        assertThat(llm.calls).isEqualTo(2);            // grounded + fallback
        assertThat(o.inputTokens()).isEqualTo(20);     // 10 + 10
        assertThat(o.outputTokens()).isEqualTo(10);    // 5 + 5
    }

    @Test void warnings_pass_through() {
        var r = new RetrievalResult(List.of(hit("t")), List.of(), false, List.of("File 'x' not found; ignoring."));
        var o = svc.generate("q", r, false);
        assertThat(o.warnings()).containsExactly("File 'x' not found; ignoring.");
    }
}
