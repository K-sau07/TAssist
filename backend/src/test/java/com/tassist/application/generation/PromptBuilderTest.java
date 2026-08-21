package com.tassist.application.generation;

import com.tassist.domain.port.out.LLMClient.LlmRequest;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

/** Unit tests for §11.5 prompt templates. */
class PromptBuilderTest {

    private final PromptBuilder pb = new PromptBuilder();

    @Test void grounded_numbers_sources_and_includes_labels() {
        LlmRequest r = pb.grounded("What is recursion?", List.of(
            new PromptBuilder.Source("lecture-04.pdf, page 7", "Recursion is a function calling itself."),
            new PromptBuilder.Source("notes.md, § Intro", "Base case stops the recursion.")));
        assertThat(r.system()).contains("[S1] (lecture-04.pdf, page 7) Recursion is a function calling itself.");
        assertThat(r.system()).contains("[S2] (notes.md, § Intro) Base case stops the recursion.");
        assertThat(r.system()).contains("answers questions strictly from the source excerpts");
        assertThat(r.messages()).hasSize(1);
        assertThat(r.messages().get(0).role()).isEqualTo("user");
        assertThat(r.messages().get(0).content()).isEqualTo("What is recursion?");
        assertThat(r.tools()).isEmpty();
    }

    @Test void grounded_embeds_exact_sentinel() {
        LlmRequest r = pb.grounded("q", List.of(new PromptBuilder.Source("f", "t")));
        assertThat(r.system()).contains(PromptBuilder.INSUFFICIENT_SENTINEL);
    }

    @Test void fallback_has_exact_prefix_instruction() {
        LlmRequest r = pb.fallback("Why is the sky blue?");
        assertThat(r.system()).contains(PromptBuilder.FALLBACK_PREFIX);
        assertThat(r.system()).contains("no relevant material was found");
        assertThat(r.messages().get(0).content()).isEqualTo("Why is the sky blue?");
    }

    @Test void regular_is_plain_assistant() {
        LlmRequest r = pb.regular("hello");
        assertThat(r.system()).contains("helpful, concise assistant");
        assertThat(r.system()).doesNotContain("excerpts");
    }
}
