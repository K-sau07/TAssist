package com.tassist.domain.vo;

import java.util.UUID;

/** Typed identifier for the Folder aggregate. Wraps a {@link UUID} so IDs are not interchangeable. */
public record FolderId(UUID value) {
    public FolderId {
        if (value == null) throw new IllegalArgumentException("FolderId value must not be null");
    }
    public static FolderId newId() { return new FolderId(UUID.randomUUID()); }
    public static FolderId of(UUID value) { return new FolderId(value); }
    public static FolderId of(String value) { return new FolderId(UUID.fromString(value)); }
}
