package com.tassist.domain.model;

import com.tassist.domain.vo.ChunkId;
import com.tassist.domain.vo.FileId;
import java.util.Map;

/**
 * A retrievable text span from a file (spec §8). The {@code embedding} length equals the
 * configured embedding dimension (locked once data exists — see BUILD_LOG D5).
 */
public record Chunk(
        ChunkId id,
        FileId fileId,
        int ordinal,
        String text,
        Map<String, String> metadata,
        float[] embedding
) {
    public Chunk {
        if (id == null) throw new IllegalArgumentException("Chunk.id must not be null");
        if (fileId == null) throw new IllegalArgumentException("Chunk.fileId must not be null");
        if (ordinal < 0) throw new IllegalArgumentException("Chunk.ordinal must not be negative");
        if (text == null) throw new IllegalArgumentException("Chunk.text must not be null");
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        // embedding may be null before the EMBEDDING stage completes; length is validated at persist time.
    }
}
