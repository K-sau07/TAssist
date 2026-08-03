package com.tassist.domain.port.out;

import java.util.List;

/**
 * Outbound port for text embedding (spec §7, §11.1 step 7, §11.4 step 3).
 *
 * <p>Adapter lives in {@code infrastructure.ai.embedding} (Voyage or OpenAI — see BUILD_LOG D5).
 * The embedding dimension is a fixed property of the chosen provider and must match the
 * {@code VECTOR(n)} column width; it cannot change once data exists.
 */
public interface EmbeddingClient {

    /** Embed a single text (e.g. a query). */
    float[] embed(String text);

    /** Embed a batch of texts (ingestion batches up to 32 per call per §11.1). Order preserved. */
    List<float[]> embedBatch(List<String> texts);

    /** The dimensionality of vectors this client produces. */
    int dimension();
}
