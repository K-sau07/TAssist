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

    /**
     * Real Anthropic SSE streaming (§11.6) via JDK HttpClient (no WebFlux dependency).
     * Parses content_block_delta (text tokens), message_start/message_delta (usage), message_stop,
     * and error events, surfacing them through {@code events}. tool_use blocks are surfaced via
     * onToolUse (the tool-use loop that acts on them is wired in the next sub-step).
     */
    @Override
    public void stream(LlmRequest request, StreamEvents events) {
        if (!props.isConfigured()) {
            events.onError("NOT_CONFIGURED", "ANTHROPIC_API_KEY not configured; cannot generate");
            return;
        }

        List<Map<String, Object>> messages = new ArrayList<>();
        for (LlmMessage m : request.messages()) {
            messages.add(Map.of("role", m.role(), "content", m.content()));
        }
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("model", props.getModel());
        body.put("max_tokens", props.getMaxTokens());
        body.put("system", request.system());
        body.put("messages", messages);
        body.put("stream", true);
        if (!request.tools().isEmpty()) {
            List<Map<String, Object>> tools = new ArrayList<>();
            for (ToolSpec t : request.tools()) {
                tools.add(Map.of("name", t.name(), "description", t.description(),
                    "input_schema", t.inputSchema()));
            }
            body.put("tools", tools);
        }

        String payload;
        try {
            payload = json.writeValueAsString(body);
        } catch (Exception e) {
            events.onError("REQUEST_SERIALIZE", e.getMessage());
            return;
        }

        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(props.getBaseUrl() + "/messages"))
            .timeout(Duration.ofSeconds(120))
            .header("x-api-key", props.getApiKey())
            .header("anthropic-version", props.getVersion())
            .header("content-type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(payload))
            .build();

        try {
            HttpResponse<java.io.InputStream> resp =
                client.send(req, HttpResponse.BodyHandlers.ofInputStream());
            if (resp.statusCode() / 100 != 2) {
                events.onError("UPSTREAM_" + resp.statusCode(), "Anthropic stream returned HTTP " + resp.statusCode());
                return;
            }
            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(resp.body(), StandardCharsets.UTF_8))) {
                parseSseStream(r, events, json);
            }
        } catch (Exception e) {
            events.onError("UPSTREAM_TIMEOUT", "Anthropic stream failed: " + e.getMessage());
        }
    }

    /**
     * Parses an Anthropic SSE stream from {@code reader}, emitting via {@code events}.
     * Package-private + static so it can be unit-tested against a canned stream (no network).
     * Emits onToken per text delta, tracks usage, and calls onCompleted at message_stop / EOF.
     */
    static void parseSseStream(BufferedReader reader, StreamEvents events, ObjectMapper json)
            throws java.io.IOException {
        int inputTokens = 0, outputTokens = 0;
        String line;
        while ((line = reader.readLine()) != null) {
            if (!line.startsWith("data:")) continue;   // ignore "event:" lines and blanks
            String data = line.substring(5).trim();
            if (data.isEmpty()) continue;
            JsonNode ev = json.readTree(data);
            switch (ev.path("type").asText()) {
                case "message_start" ->
                    inputTokens = ev.path("message").path("usage").path("input_tokens").asInt(0);
                case "content_block_delta" -> {
                    JsonNode delta = ev.path("delta");
                    if ("text_delta".equals(delta.path("type").asText())) {
                        events.onToken(delta.path("text").asText());
                    }
                }
                case "message_delta" ->
                    outputTokens = ev.path("usage").path("output_tokens").asInt(outputTokens);
                case "error" ->
                    events.onError("UPSTREAM_ERROR",
                        ev.path("error").path("message").asText("stream error"));
                default -> { /* content_block_start/stop, ping, message_stop — ignore */ }
            }
        }
        events.onCompleted(inputTokens, outputTokens);
    }
}
