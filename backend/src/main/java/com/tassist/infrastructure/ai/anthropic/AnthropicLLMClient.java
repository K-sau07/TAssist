package com.tassist.infrastructure.ai.anthropic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tassist.domain.error.UpstreamError;
import com.tassist.domain.port.out.LLMClient;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Anthropic Messages API adapter for Claude Haiku (§11.6). Direct RestClient call (mirrors the
 * Voyage adapter pattern, D6) — full control over system/messages/tools + token usage.
 * Fails loudly if unconfigured or on API error; no fake fallback. Non-streaming (Step 9);
 * stream() arrives in Step 11.
 */
@Component
public class AnthropicLLMClient implements LLMClient {

    private final AnthropicProperties props;
    private final RestClient http;
    private final ObjectMapper json = new ObjectMapper();

    public AnthropicLLMClient(AnthropicProperties props) {
        this.props = props;
        this.http = RestClient.builder().baseUrl(props.getBaseUrl()).build();
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        if (!props.isConfigured())
            throw new IllegalStateException("ANTHROPIC_API_KEY not configured; cannot generate");

        List<Map<String, Object>> messages = new ArrayList<>();
        for (LlmMessage m : request.messages()) {
            messages.add(Map.of("role", m.role(), "content", m.content()));
        }
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("model", props.getModel());
        body.put("max_tokens", props.getMaxTokens());
        body.put("system", request.system());
        body.put("messages", messages);
        if (!request.tools().isEmpty()) {
            List<Map<String, Object>> tools = new ArrayList<>();
            for (ToolSpec t : request.tools()) {
                tools.add(Map.of("name", t.name(), "description", t.description(),
                    "input_schema", t.inputSchema()));
            }
            body.put("tools", tools);
        }

        String raw;
        try {
            raw = http.post()
                .uri("/messages")
                .header("x-api-key", props.getApiKey())
                .header("anthropic-version", props.getVersion())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);
        } catch (RuntimeException e) {
            throw new UpstreamError.LlmFailure("Anthropic API call failed: " + e.getMessage(), e);
        }

        try {
            JsonNode root = json.readTree(raw);
            StringBuilder text = new StringBuilder();
            for (JsonNode block : root.path("content")) {
                if ("text".equals(block.path("type").asText())) {
                    text.append(block.path("text").asText());
                }
            }
            JsonNode usage = root.path("usage");
            int in = usage.path("input_tokens").asInt(0);
            int out = usage.path("output_tokens").asInt(0);
            return new LlmResponse(text.toString(), in, out);
        } catch (Exception e) {
            throw new UpstreamError.LlmFailure("Anthropic response parse failed: " + e.getMessage(), e);
        }
    }
    // ---- streaming ----

    /** Plain streaming (no tool loop): forward text tokens + usage. Used by callers without tools. */
    @Override
    public void stream(LlmRequest request, StreamEvents events) {
        streamWithTools(request, events, null);
    }

    /**
     * Streaming WITH a server-side tool-use loop (§11.6 step 5 / §11.7). When the model finishes a
     * turn with stop_reason=tool_use, each tool_use block is executed via {@code toolExecutor}, the
     * results are appended as a user tool_result turn, and the conversation is re-streamed until a
     * natural end. Text tokens stream through {@code events.onToken} across all turns.
     */
    @Override
    public void stream(LlmRequest request, StreamEvents events, ToolExecutor toolExecutor) {
        streamWithTools(request, events, toolExecutor);
    }

    private void streamWithTools(LlmRequest request, StreamEvents events, ToolExecutor toolExecutor) {
        if (!props.isConfigured()) {
            events.onError("NOT_CONFIGURED", "ANTHROPIC_API_KEY not configured; cannot generate");
            return;
        }
        // running message list (raw maps) so we can append assistant tool_use + user tool_result turns
        List<Map<String, Object>> messages = new ArrayList<>();
        for (LlmMessage m : request.messages()) {
            messages.add(mapOf("role", m.role(), "content", m.content()));
        }

        int totalIn = 0, totalOut = 0;
        int guard = 0;
        try {
            while (true) {
                if (++guard > 6) { // safety: cap tool-use round-trips
                    events.onError("TOOL_LOOP_LIMIT", "exceeded max tool-use iterations");
                    break;
                }
                StreamTurn turn = sendStreamOnce(request, messages, events);
                totalIn += turn.inputTokens;
                totalOut += turn.outputTokens;

                if (turn.error != null) { events.onError(turn.errorCode, turn.error); return; }

                if (!turn.toolCalls.isEmpty() && toolExecutor != null) {
                    // append assistant turn (its tool_use blocks) then user turn (tool_result blocks)
                    messages.add(mapOf("role", "assistant", "content", turn.assistantContent));
                    List<Map<String, Object>> results = new ArrayList<>();
                    for (ToolCall call : turn.toolCalls) {
                        events.onToolUse(call.id(), call.name(), call.input());
                        Map<String, Object> result = toolExecutor.execute(call);
                        Map<String, Object> block = new LinkedHashMap<>();
                        block.put("type", "tool_result");
                        block.put("tool_use_id", call.id());
                        block.put("content", safeJson(result));
                        results.add(block);
                    }
                    messages.add(mapOf("role", "user", "content", results));
                    continue; // re-stream with the tool results in context
                }
                break; // natural end (no tool calls, or no executor)
            }
        } catch (Exception e) {
            events.onError("UPSTREAM_TIMEOUT", "Anthropic stream failed: " + e.getMessage());
            return;
        }
        events.onCompleted(totalIn, totalOut);
    }

    /** One streaming round-trip. Streams text via events.onToken; collects tool_use blocks + usage. */
    private StreamTurn sendStreamOnce(LlmRequest request, List<Map<String, Object>> messages,
                                      StreamEvents events) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("model", props.getModel());
        body.put("max_tokens", props.getMaxTokens());
        body.put("system", request.system());
        body.put("messages", messages);
        body.put("stream", true);
        if (!request.tools().isEmpty()) {
            List<Map<String, Object>> tools = new ArrayList<>();
            for (ToolSpec t : request.tools()) {
                tools.add(mapOf("name", t.name(), "description", t.description(),
                    "input_schema", t.inputSchema()));
            }
            body.put("tools", tools);
        }
        String payload = json.writeValueAsString(body);

        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(props.getBaseUrl() + "/messages"))
            .timeout(Duration.ofSeconds(120))
            .header("x-api-key", props.getApiKey())
            .header("anthropic-version", props.getVersion())
            .header("content-type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(payload))
            .build();

        HttpResponse<java.io.InputStream> resp = client.send(req, HttpResponse.BodyHandlers.ofInputStream());
        StreamTurn turn = new StreamTurn();
        if (resp.statusCode() / 100 != 2) {
            turn.errorCode = "UPSTREAM_" + resp.statusCode();
            turn.error = "Anthropic stream returned HTTP " + resp.statusCode();
            return turn;
        }
        try (BufferedReader r = new BufferedReader(new InputStreamReader(resp.body(), StandardCharsets.UTF_8))) {
            parseTurn(r, events, json, turn);
        }
        return turn;
    }

    /** Accumulates one turn: text tokens (streamed live), tool_use blocks, usage, assistant content array. */
    static void parseTurn(BufferedReader reader, StreamEvents events, ObjectMapper json, StreamTurn turn)
            throws java.io.IOException {
        Map<Integer, BlockAcc> blocks = new HashMap<>();
        String line;
        while ((line = reader.readLine()) != null) {
            if (!line.startsWith("data:")) continue;
            String data = line.substring(5).trim();
            if (data.isEmpty()) continue;
            JsonNode ev = json.readTree(data);
            switch (ev.path("type").asText()) {
                case "message_start" ->
                    turn.inputTokens = ev.path("message").path("usage").path("input_tokens").asInt(0);
                case "content_block_start" -> {
                    int idx = ev.path("index").asInt();
                    JsonNode cb = ev.path("content_block");
                    BlockAcc acc = new BlockAcc();
                    acc.type = cb.path("type").asText();
                    if ("tool_use".equals(acc.type)) {
                        acc.id = cb.path("id").asText();
                        acc.name = cb.path("name").asText();
                    } else if ("text".equals(acc.type)) {
                        acc.text.append(cb.path("text").asText(""));
                    }
                    blocks.put(idx, acc);
                }
                case "content_block_delta" -> {
                    int idx = ev.path("index").asInt();
                    BlockAcc acc = blocks.computeIfAbsent(idx, k -> new BlockAcc());
                    JsonNode delta = ev.path("delta");
                    String dt = delta.path("type").asText();
                    if ("text_delta".equals(dt)) {
                        String t = delta.path("text").asText();
                        acc.text.append(t);
                        events.onToken(t);
                    } else if ("input_json_delta".equals(dt)) {
                        acc.jsonBuf.append(delta.path("partial_json").asText());
                    }
                }
                case "message_delta" -> {
                    turn.outputTokens = ev.path("usage").path("output_tokens").asInt(turn.outputTokens);
                    String sr = ev.path("delta").path("stop_reason").asText("");
                    if (!sr.isEmpty()) turn.stopReason = sr;
                }
                case "error" -> {
                    turn.errorCode = "UPSTREAM_ERROR";
                    turn.error = ev.path("error").path("message").asText("stream error");
                }
                default -> { /* content_block_stop, ping, message_stop */ }
            }
        }
        // assemble tool calls + assistant content array (for the follow-up turn)
        List<Map<String, Object>> assistantContent = new ArrayList<>();
        for (Map.Entry<Integer, BlockAcc> e : new java.util.TreeMap<>(blocks).entrySet()) {
            BlockAcc acc = e.getValue();
            if ("text".equals(acc.type) && acc.text.length() > 0) {
                Map<String, Object> b = new LinkedHashMap<>();
                b.put("type", "text"); b.put("text", acc.text.toString());
                assistantContent.add(b);
            } else if ("tool_use".equals(acc.type)) {
                Map<String, Object> input = parseJsonObj(json, acc.jsonBuf.toString());
                turn.toolCalls.add(new ToolCall(acc.id, acc.name, input));
                Map<String, Object> b = new LinkedHashMap<>();
                b.put("type", "tool_use"); b.put("id", acc.id); b.put("name", acc.name); b.put("input", input);
                assistantContent.add(b);
            }
        }
        turn.assistantContent = assistantContent;
    }

    // ---- helpers/types ----
    static final class StreamTurn {
        int inputTokens, outputTokens;
        String stopReason, error, errorCode;
        final List<ToolCall> toolCalls = new ArrayList<>();
        Object assistantContent = List.of();
    }
    static final class BlockAcc {
        String type, id, name;
        final StringBuilder text = new StringBuilder();
        final StringBuilder jsonBuf = new StringBuilder();
    }

    private static Map<String, Object> mapOf(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }
    static Map<String, Object> parseJsonObj(ObjectMapper json, String raw) {
        try {
            if (raw == null || raw.isBlank()) return Map.of();
            return json.readValue(raw, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
        } catch (Exception e) { return Map.of(); }
    }
    private String safeJson(Map<String, Object> result) {
        try { return json.writeValueAsString(result); }
        catch (Exception e) { return "{\"error\":\"result serialization failed\"}"; }
    }
}
