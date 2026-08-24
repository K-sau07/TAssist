package com.tassist.domain.vo;

import java.util.UUID;

/** Typed identifier for the Conversation aggregate. Wraps a {@link UUID} so IDs are not interchangeable. */
public record ConversationId(UUID value) {
    public ConversationId {
        if (value == null) throw new IllegalArgumentException("ConversationId value must not be null");
    }
    public static ConversationId newId() { return new ConversationId(UUID.randomUUID()); }
    public static ConversationId of(UUID value) { return new ConversationId(value); }
    public static ConversationId of(String value) { return new ConversationId(UUID.fromString(value)); }
}
