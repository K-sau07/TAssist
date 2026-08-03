package com.tassist.domain.port.out;

import com.tassist.domain.model.Chunk;
import com.tassist.domain.vo.FileId;
import java.util.List;

/**
 * Persistence + vector-search port for {@link Chunk} (spec §7, §11.4).
 * The pgvector similarity query adapter lives in {@code infrastructure.persistence.vector}.
 */
public interface ChunkRepository {

    /** Insert all chunks for a file atomically (§11.1: all-or-nothing). */
    void saveAll(List<Chunk> chunks);

    long countByFile(FileId fileId);

    void deleteByFile(FileId fileId);

    /** Cosine-similarity search over chunks restricted to {@code candidateFileIds} (§11.4 step 5). */
    List<ScoredChunk> searchSimilar(float[] queryEmbedding, List<FileId> candidateFileIds, int topK);

    /** A chunk paired with its similarity score (1 - cosine distance). */
    record ScoredChunk(Chunk chunk, double similarity) {}
}
