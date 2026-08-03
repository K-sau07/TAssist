package com.tassist.domain.vo;

import java.util.UUID;

/** Typed identifier for the User aggregate. Wraps a {@link UUID} so IDs are not interchangeable. */
public record UserId(UUID value) {
    public UserId {
        if (value == null) throw new IllegalArgumentException("UserId value must not be null");
    }
    public static UserId newId() { return new UserId(UUID.randomUUID()); }
    public static UserId of(UUID value) { return new UserId(value); }
    public static UserId of(String value) { return new UserId(UUID.fromString(value)); }
}
