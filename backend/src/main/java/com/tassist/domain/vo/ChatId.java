package com.tassist.domain.vo;

import java.util.UUID;

/** Typed identifier for the Chat aggregate. Wraps a {@link UUID} so IDs are not interchangeable. */
public record ChatId(UUID value) {
    public ChatId {
        if (value == null) throw new IllegalArgumentException("ChatId value must not be null");
    }
    public static ChatId newId() { return new ChatId(UUID.randomUUID()); }
    public static ChatId of(UUID value) { return new ChatId(value); }
    public static ChatId of(String value) { return new ChatId(UUID.fromString(value)); }
}
