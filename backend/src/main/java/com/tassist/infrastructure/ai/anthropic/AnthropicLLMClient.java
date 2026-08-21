package com.tassist.infrastructure.ai.anthropic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tassist.domain.error.UpstreamError;
import com.tassist.domain.port.out.LLMClient;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

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

    @Override
    public void stream(LlmRequest request, StreamEvents events) {
        throw new UnsupportedOperationException("streaming arrives in Step 11");
    }
}
