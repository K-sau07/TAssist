package com.tassist.domain.vo;

import java.util.UUID;

/** Typed identifier for the Chunk aggregate. Wraps a {@link UUID} so IDs are not interchangeable. */
public record ChunkId(UUID value) {
    public ChunkId {
        if (value == null) throw new IllegalArgumentException("ChunkId value must not be null");
    }
    public static ChunkId newId() { return new ChunkId(UUID.randomUUID()); }
    public static ChunkId of(UUID value) { return new ChunkId(value); }
    public static ChunkId of(String value) { return new ChunkId(UUID.fromString(value)); }
}
