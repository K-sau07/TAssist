package com.tassist.infrastructure.persistence.adapter;

import com.tassist.domain.model.Chunk;
import com.tassist.domain.port.out.ChunkRepository;
import com.tassist.domain.vo.ChunkId;
import com.tassist.domain.vo.FileId;
import com.tassist.infrastructure.persistence.mapper.ChunkMapper;
import com.tassist.infrastructure.persistence.repo.ChunkJpaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public class ChunkRepositoryAdapter implements ChunkRepository {
    private final ChunkJpaRepository jpa;
    @PersistenceContext private EntityManager em;

    public ChunkRepositoryAdapter(ChunkJpaRepository jpa) { this.jpa = jpa; }

    @Override @Transactional
    public void saveAll(List<Chunk> chunks) {
        jpa.saveAll(chunks.stream().map(ChunkMapper::toEntity).toList());
    }

    @Override public long countByFile(FileId fileId) { return jpa.countByFileId(fileId.value()); }

    @Override @Transactional
    public void deleteByFile(FileId fileId) { jpa.deleteByFileId(fileId.value()); }

    @Override
    public List<ScoredChunk> searchSimilar(float[] queryEmbedding, List<FileId> candidateFileIds, int topK) {
        if (candidateFileIds.isEmpty()) return List.of();
        String vec = toVectorLiteral(queryEmbedding);
        List<UUID> ids = candidateFileIds.stream().map(FileId::value).toList();
        @SuppressWarnings("unchecked")
        List<Tuple> rows = em.createNativeQuery(
                "SELECT id, file_id, ordinal, text, metadata, " +
                "(1 - (embedding <=> CAST(:q AS vector))) AS similarity " +
                "FROM chunk WHERE file_id IN (:ids) " +
                "ORDER BY embedding <=> CAST(:q AS vector) LIMIT :k", Tuple.class)
            .setParameter("q", vec)
            .setParameter("ids", ids)
            .setParameter("k", topK)
            .getResultList();
        return rows.stream().map(t -> {
            Chunk c = new Chunk(
                ChunkId.of((UUID) t.get("id")),
                FileId.of((UUID) t.get("file_id")),
                ((Number) t.get("ordinal")).intValue(),
                (String) t.get("text"),
                Map.of(),           // metadata not needed for retrieval scoring; kept minimal
                null                // embedding not re-read on search path
            );
            double sim = ((Number) t.get("similarity")).doubleValue();
            return new ScoredChunk(c, sim);
        }).toList();
    }

    private static String toVectorLiteral(float[] v) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < v.length; i++) { if (i > 0) sb.append(','); sb.append(v[i]); }
        return sb.append(']').toString();
    }
}
