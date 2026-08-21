package com.tassist.infrastructure.ai.anthropic;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tassist.domain.port.out.LLMClient.StreamEvents;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Unit test for §11.6 SSE parsing — canned Anthropic stream, no network. */
class AnthropicStreamParseTest {

    private final ObjectMapper json = new ObjectMapper();

    static class CapturingEvents implements StreamEvents {
        final List<String> tokens = new ArrayList<>();
        int inTok = -1, outTok = -1;
        String errCode, errMsg;
        boolean completed;
        public void onToken(String t) { tokens.add(t); }
        public void onToolUse(String id, String name, Map<String,Object> in) {}
        public void onCompleted(int in, int out) { completed = true; inTok = in; outTok = out; }
        public void onError(String code, String msg) { errCode = code; errMsg = msg; }
    }

    private BufferedReader sse(String... dataLines) {
        StringBuilder sb = new StringBuilder();
        for (String d : dataLines) sb.append("event: x\n").append("data: ").append(d).append("\n\n");
        return new BufferedReader(new StringReader(sb.toString()));
    }

    @Test void parses_tokens_in_order_and_tracks_usage() throws Exception {
        var ev = new CapturingEvents();
        AnthropicLLMClient.parseSseStream(sse(
            "{\"type\":\"message_start\",\"message\":{\"usage\":{\"input_tokens\":42}}}",
            "{\"type\":\"content_block_start\",\"index\":0}",
            "{\"type\":\"content_block_delta\",\"delta\":{\"type\":\"text_delta\",\"text\":\"Hello\"}}",
            "{\"type\":\"content_block_delta\",\"delta\":{\"type\":\"text_delta\",\"text\":\" world\"}}",
            "{\"type\":\"content_block_delta\",\"delta\":{\"type\":\"text_delta\",\"text\":\" [S1]\"}}",
            "{\"type\":\"message_delta\",\"usage\":{\"output_tokens\":7}}",
            "{\"type\":\"message_stop\"}"
        ), ev, json);

        assertThat(ev.tokens).containsExactly("Hello", " world", " [S1]");
        assertThat(ev.completed).isTrue();
        assertThat(ev.inTok).isEqualTo(42);
        assertThat(ev.outTok).isEqualTo(7);
        assertThat(ev.errCode).isNull();
    }

    @Test void ignores_ping_and_non_data_lines() throws Exception {
        var ev = new CapturingEvents();
        AnthropicLLMClient.parseSseStream(sse(
            "{\"type\":\"ping\"}",
            "{\"type\":\"content_block_delta\",\"delta\":{\"type\":\"text_delta\",\"text\":\"hi\"}}",
            "{\"type\":\"message_stop\"}"
        ), ev, json);
        assertThat(ev.tokens).containsExactly("hi");
        assertThat(ev.completed).isTrue();
    }

    @Test void surfaces_error_event() throws Exception {
        var ev = new CapturingEvents();
        AnthropicLLMClient.parseSseStream(sse(
            "{\"type\":\"error\",\"error\":{\"message\":\"overloaded\"}}"
        ), ev, json);
        assertThat(ev.errCode).isEqualTo("UPSTREAM_ERROR");
        assertThat(ev.errMsg).isEqualTo("overloaded");
    }
}
