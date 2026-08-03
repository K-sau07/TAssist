package com.tassist.domain.vo;

import java.util.UUID;

/** Typed identifier for the Message aggregate. Wraps a {@link UUID} so IDs are not interchangeable. */
public record MessageId(UUID value) {
    public MessageId {
        if (value == null) throw new IllegalArgumentException("MessageId value must not be null");
    }
    public static MessageId newId() { return new MessageId(UUID.randomUUID()); }
    public static MessageId of(UUID value) { return new MessageId(value); }
    public static MessageId of(String value) { return new MessageId(UUID.fromString(value)); }
}
