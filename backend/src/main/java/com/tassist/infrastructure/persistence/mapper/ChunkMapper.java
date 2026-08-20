package com.tassist.infrastructure.persistence.mapper;

import com.tassist.domain.model.Chunk;
import com.tassist.domain.vo.ChunkId;
import com.tassist.domain.vo.FileId;
import com.tassist.infrastructure.persistence.entity.ChunkEntity;
import java.time.Instant;

public final class ChunkMapper {
    private ChunkMapper() {}

    public static ChunkEntity toEntity(Chunk c) {
        ChunkEntity e = new ChunkEntity();
        e.setId(c.id().value());
        e.setFileId(c.fileId().value());
        e.setOrdinal(c.ordinal());
        e.setText(c.text());
        e.setMetadata(c.metadata());
        e.setEmbedding(c.embedding());
        e.setCreatedAt(Instant.now());
        return e;
    }

    public static Chunk toDomain(ChunkEntity e) {
        return new Chunk(
            ChunkId.of(e.getId()),
            FileId.of(e.getFileId()),
            e.getOrdinal(),
            e.getText(),
            e.getMetadata(),
            e.getEmbedding()
        );
    }
}
