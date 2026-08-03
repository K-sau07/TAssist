package com.tassist.domain.port.out;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Outbound port for the chat LLM (Claude Haiku) (spec §7, §11.6).
 *
 * <p>Adapter lives in {@code infrastructure.ai.anthropic}. Per architectural invariant §7.1,
 * the only content ever passed here is: a system prompt, retrieved chunk texts, and the
 * user's question — never raw files or full documents.
 *
 * <p>Two modes: {@link #complete} (non-streaming, built first in Step 9) and
 * {@link #stream} (SSE token streaming with server-side tool-use, Step 11).
 */
public interface LLMClient {

    /** A single message in an LLM request. */
    record LlmMessage(String role, String content) {
        public LlmMessage {
            if (role == null || role.isBlank()) throw new IllegalArgumentException("LlmMessage.role must not be blank");
            if (content == null) throw new IllegalArgumentException("LlmMessage.content must not be null");
        }
    }

    /** A tool the model may call (e.g. {@code query_spreadsheet}). */
    record ToolSpec(String name, String description, Map<String, Object> inputSchema) {}

    /** Request to the LLM: system prompt + conversation + optional tools. */
    record LlmRequest(String system, List<LlmMessage> messages, List<ToolSpec> tools) {
        public LlmRequest {
            if (system == null) throw new IllegalArgumentException("LlmRequest.system must not be null");
            messages = messages == null ? List.of() : List.copyOf(messages);
            tools = tools == null ? List.of() : List.copyOf(tools);
        }
    }

    /** Full (non-streaming) response. */
    record LlmResponse(String content, int inputTokens, int outputTokens) {}

    /**
     * Streaming events surfaced to the caller. The application/web layer translates these
     * into the SSE schema of §11.6. A tool-use event pauses token flow until the caller
     * supplies a tool result back through the returned handle (wired in Step 11).
     */
    interface StreamEvents {
        void onToken(String text);
        void onToolUse(String toolCallId, String name, Map<String, Object> input);
        void onCompleted(int inputTokens, int outputTokens);
        void onError(String code, String message);
    }

    /** Non-streaming completion (Step 9). */
    LlmResponse complete(LlmRequest request);

    /** Streaming completion (Step 11). Blocks until generation completes, emitting via {@code events}. */
    void stream(LlmRequest request, StreamEvents events);
}
