package com.tassist.infrastructure.ai.embedding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tassist.domain.port.out.EmbeddingClient;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Voyage embedding adapter (D5/D16): POST {baseUrl}/embeddings, model voyage-3.5, 1024-dim.
 * Batches of up to 32 per call (§11.1). Fails loudly (UpstreamError-style) if unconfigured
 * or on API error — no fake fallback.
 */
@Component
public class VoyageEmbeddingClient implements EmbeddingClient {

    private static final int MAX_BATCH = 32;

    private final VoyageProperties props;
    private final RestClient http;
    private final ObjectMapper json = new ObjectMapper();

    public VoyageEmbeddingClient(VoyageProperties props) {
        this.props = props;
        this.http = RestClient.builder().baseUrl(props.getBaseUrl()).build();
    }

    @Override
    public float[] embed(String text) {
        return embedBatch(List.of(text)).get(0);
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        if (!props.isConfigured())
            throw new IllegalStateException("VOYAGE_API_KEY not configured; cannot embed");
        List<float[]> out = new ArrayList<>(texts.size());
        for (int i = 0; i < texts.size(); i += MAX_BATCH) {
            out.addAll(callApi(texts.subList(i, Math.min(i + MAX_BATCH, texts.size()))));
        }
        return out;
    }

    private List<float[]> callApi(List<String> batch) {
        Map<String, Object> body = Map.of(
            "input", batch,
            "model", props.getModel(),
            "input_type", "document",
            "output_dimension", props.getDimension()
        );
        String resp = http.post()
            .uri("/embeddings")
            .header("Authorization", "Bearer " + props.getApiKey())
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .body(String.class);
        try {
            JsonNode data = json.readTree(resp).path("data");
            // Voyage returns items with an "index" field; sort by it to guarantee order.
            List<float[]> result = new ArrayList<>(batch.size());
            for (int k = 0; k < batch.size(); k++) result.add(null);
            for (JsonNode item : data) {
                int idx = item.path("index").asInt();
                JsonNode emb = item.path("embedding");
                float[] vec = new float[emb.size()];
                for (int j = 0; j < emb.size(); j++) vec[j] = (float) emb.get(j).asDouble();
                if (vec.length != props.getDimension())
                    throw new IllegalStateException("embedding dim " + vec.length
                        + " != expected " + props.getDimension());
                result.set(idx, vec);
            }
            return result;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("failed to parse Voyage response", e);
        }
    }

    @Override
    public int dimension() { return props.getDimension(); }
}
