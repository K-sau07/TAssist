package com.tassist.infrastructure.ai.anthropic;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tassist.domain.port.out.LLMClient.StreamEvents;
import com.tassist.domain.port.out.LLMClient.ToolCall;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Unit test for §11.6 SSE turn parsing (text tokens + tool_use accumulation), no network. */
class AnthropicStreamParseTest {

    private final ObjectMapper json = new ObjectMapper();

    static class CapturingEvents implements StreamEvents {
        final List<String> tokens = new ArrayList<>();
        final List<ToolCall> toolUses = new ArrayList<>();
        String errCode, errMsg;
        public void onToken(String t) { tokens.add(t); }
        public void onToolUse(String id, String name, Map<String,Object> in) { toolUses.add(new ToolCall(id,name,in)); }
        public void onCompleted(int in, int out) {}
        public void onError(String code, String msg) { errCode = code; errMsg = msg; }
    }

    private BufferedReader sse(String... dataLines) {
        StringBuilder sb = new StringBuilder();
        for (String d : dataLines) sb.append("event: x\n").append("data: ").append(d).append("\n\n");
        return new BufferedReader(new StringReader(sb.toString()));
    }

    @Test void parses_text_tokens_in_order_and_usage() throws Exception {
        var ev = new CapturingEvents();
        var turn = new AnthropicLLMClient.StreamTurn();
        AnthropicLLMClient.parseTurn(sse(
            "{\"type\":\"message_start\",\"message\":{\"usage\":{\"input_tokens\":42}}}",
            "{\"type\":\"content_block_start\",\"index\":0,\"content_block\":{\"type\":\"text\",\"text\":\"\"}}",
            "{\"type\":\"content_block_delta\",\"index\":0,\"delta\":{\"type\":\"text_delta\",\"text\":\"Hello\"}}",
            "{\"type\":\"content_block_delta\",\"index\":0,\"delta\":{\"type\":\"text_delta\",\"text\":\" world [S1]\"}}",
            "{\"type\":\"message_delta\",\"usage\":{\"output_tokens\":7},\"delta\":{\"stop_reason\":\"end_turn\"}}",
            "{\"type\":\"message_stop\"}"
        ), ev, json, turn);

        assertThat(ev.tokens).containsExactly("Hello", " world [S1]");
        assertThat(turn.inputTokens).isEqualTo(42);
        assertThat(turn.outputTokens).isEqualTo(7);
        assertThat(turn.stopReason).isEqualTo("end_turn");
        assertThat(turn.toolCalls).isEmpty();
    }

    @Test void accumulates_tool_use_input_json_and_surfaces_call() throws Exception {
        var ev = new CapturingEvents();
        var turn = new AnthropicLLMClient.StreamTurn();
        AnthropicLLMClient.parseTurn(sse(
            "{\"type\":\"content_block_start\",\"index\":0,\"content_block\":{\"type\":\"tool_use\",\"id\":\"tu_1\",\"name\":\"query_spreadsheet\",\"input\":{}}}",
            "{\"type\":\"content_block_delta\",\"index\":0,\"delta\":{\"type\":\"input_json_delta\",\"partial_json\":\"{\\\"sheet_id\\\":\"}}",
            "{\"type\":\"content_block_delta\",\"index\":0,\"delta\":{\"type\":\"input_json_delta\",\"partial_json\":\"\\\"abc\\\"}\"}}",
            "{\"type\":\"content_block_stop\",\"index\":0}",
            "{\"type\":\"message_delta\",\"usage\":{\"output_tokens\":12},\"delta\":{\"stop_reason\":\"tool_use\"}}",
            "{\"type\":\"message_stop\"}"
        ), ev, json, turn);

        assertThat(turn.stopReason).isEqualTo("tool_use");
        assertThat(turn.toolCalls).hasSize(1);
        ToolCall call = turn.toolCalls.get(0);
        assertThat(call.id()).isEqualTo("tu_1");
        assertThat(call.name()).isEqualTo("query_spreadsheet");
        assertThat(call.input()).containsEntry("sheet_id", "abc");
    }

    @Test void surfaces_error_event() throws Exception {
        var ev = new CapturingEvents();
        var turn = new AnthropicLLMClient.StreamTurn();
        AnthropicLLMClient.parseTurn(sse(
            "{\"type\":\"error\",\"error\":{\"message\":\"overloaded\"}}"
        ), ev, json, turn);
        assertThat(turn.errorCode).isEqualTo("UPSTREAM_ERROR");
        assertThat(turn.error).isEqualTo("overloaded");
    }
}
