package com.tassist.application.generation;

import com.tassist.domain.model.File;
import com.tassist.domain.vo.FileId;
import com.tassist.domain.port.in.RetrievalUseCase.RetrievalResult;
import com.tassist.domain.port.in.RetrievalUseCase.TextHit;
import com.tassist.domain.port.out.FileRepository;
import com.tassist.domain.port.out.LLMClient;
import com.tassist.domain.port.out.LLMClient.LlmResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Non-streaming generation (§11.5/§11.6 non-stream path, Step 9). Picks a prompt mode from the
 * retrieval outcome, calls the LLM, and applies the grounded→fallback rerun when the model
 * returns the "insufficient sources" sentinel. Streaming + tool-use arrive in Step 11.
 */
@Service
public class GenerationService {

    private static final Logger log = LoggerFactory.getLogger(GenerationService.class);

    public enum Mode { GROUNDED, FALLBACK, REGULAR }

    private final PromptBuilder prompts;
    private final LLMClient llm;
    private final FileRepository files;

    public GenerationService(PromptBuilder prompts, LLMClient llm, FileRepository files) {
        this.prompts = prompts;
        this.llm = llm;
        this.files = files;
    }

    /** Result of a non-streaming generation: the answer, the mode actually used, and token counts. */
    public record GenerationOutcome(String answer, Mode mode, int inputTokens, int outputTokens,
                                    List<String> warnings) {}

    /**
     * @param retrieval may be null/empty for REGULAR scope.
     * @param regularScope true when the chat scope is REGULAR (no retrieval requested at all).
     */
    public GenerationOutcome generate(String question, RetrievalResult retrieval, boolean regularScope) {
        List<String> warnings = retrieval == null ? List.of() : retrieval.warnings();

        // Regular mode: pure Claude, no retrieval machinery.
        if (regularScope) {
            LlmResponse r = llm.complete(prompts.regular(question));
            return new GenerationOutcome(r.content(), Mode.REGULAR, r.inputTokens(), r.outputTokens(), warnings);
        }

        boolean haveHits = retrieval != null
            && (!retrieval.textHits().isEmpty() || !retrieval.spreadsheetHits().isEmpty());

        // No usable hits → fallback straight away (§11.5 fallback mode).
        if (!haveHits) {
            return runFallback(question, warnings);
        }

        // Grounded mode.
        List<PromptBuilder.Source> sources = buildSources(retrieval.textHits(), null);
        LlmResponse r = llm.complete(prompts.grounded(question, sources));

        // §11.6 step 7: if grounded returns the sentinel, rerun in fallback mode.
        if (r.content() != null && r.content().strip().equals(PromptBuilder.INSUFFICIENT_SENTINEL)) {
            log.info("Grounded answer hit sentinel; rerunning in fallback mode.");
            GenerationOutcome fb = runFallback(question, warnings);
            // token cost includes both calls
            return new GenerationOutcome(fb.answer(), Mode.FALLBACK,
                r.inputTokens() + fb.inputTokens(), r.outputTokens() + fb.outputTokens(), warnings);
        }

        return new GenerationOutcome(r.content(), Mode.GROUNDED, r.inputTokens(), r.outputTokens(), warnings);
    }

    /**
     * Streaming support (§11.6): resolve the prompt mode + the LlmRequest to stream, WITHOUT calling
     * the LLM. ChatStreamService uses this so streaming shares mode-selection with non-stream.
     * Returns GROUNDED (with sources), FALLBACK, or REGULAR. Grounded→sentinel rerun is handled by
     * the streaming caller (it can only detect the sentinel after the stream completes).
     */
    public record Plan(Mode mode, com.tassist.domain.port.out.LLMClient.LlmRequest request,
                       List<PromptBuilder.Source> sources) {}

    public Plan plan(String question, RetrievalResult retrieval, boolean regularScope) {
        return plan(question, retrieval, regularScope, null);
    }

    /** {@code labelFor} overrides the source label per file (channel chats → display_label, §11.8). */
    public Plan plan(String question, RetrievalResult retrieval, boolean regularScope,
                     Function<FileId, String> labelFor) {
        if (regularScope) {
            return new Plan(Mode.REGULAR, prompts.regular(question), List.of());
        }
        boolean haveHits = retrieval != null
            && (!retrieval.textHits().isEmpty() || !retrieval.spreadsheetHits().isEmpty());
        if (!haveHits) {
            return new Plan(Mode.FALLBACK, prompts.fallback(question), List.of());
        }
        List<PromptBuilder.Source> sources = buildSources(retrieval.textHits(), labelFor);
        return new Plan(Mode.GROUNDED, prompts.grounded(question, sources), sources);
    }

    /** Fallback request builder, exposed for the streaming sentinel rerun. */
    public com.tassist.domain.port.out.LLMClient.LlmRequest fallbackRequest(String question) {
        return prompts.fallback(question);
    }

    private GenerationOutcome runFallback(String question, List<String> warnings) {
        LlmResponse r = llm.complete(prompts.fallback(question));
        return new GenerationOutcome(r.content(), Mode.FALLBACK, r.inputTokens(), r.outputTokens(), warnings);
    }

    /** Build numbered sources with §11.8 labels. If {@code labelFor} is set (channel chats), it supplies
     *  the base label (display_label); otherwise the private-library filename is used. Positional hint added either way. */
    private List<PromptBuilder.Source> buildSources(List<TextHit> hits, Function<FileId, String> labelFor) {
        List<PromptBuilder.Source> sources = new ArrayList<>(hits.size());
        for (TextHit hit : hits) {
            File f = files.findById(hit.chunk().fileId()).orElse(null);
            com.tassist.domain.vo.FileType type = f != null ? f.type() : com.tassist.domain.vo.FileType.TXT;
            String base = labelFor != null ? labelFor.apply(hit.chunk().fileId())
                : (f != null ? f.originalFilename() : "source");
            String label = CitationLabeler.label(base, type, hit.chunk().metadata());
            sources.add(new PromptBuilder.Source(label, hit.chunk().text()));
        }
        return sources;
    }
}
