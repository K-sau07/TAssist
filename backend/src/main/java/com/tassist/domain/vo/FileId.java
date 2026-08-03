package com.tassist.domain.vo;

import java.util.UUID;

/** Typed identifier for the File aggregate. Wraps a {@link UUID} so IDs are not interchangeable. */
public record FileId(UUID value) {
    public FileId {
        if (value == null) throw new IllegalArgumentException("FileId value must not be null");
    }
    public static FileId newId() { return new FileId(UUID.randomUUID()); }
    public static FileId of(UUID value) { return new FileId(value); }
    public static FileId of(String value) { return new FileId(UUID.fromString(value)); }
}
