package com.tassist.domain.vo;

import java.util.UUID;

/** Typed identifier for a ConversationMessage. Wraps a {@link UUID} so IDs are not interchangeable. */
public record ConversationMessageId(UUID value) {
    public ConversationMessageId {
        if (value == null) throw new IllegalArgumentException("ConversationMessageId value must not be null");
    }
    public static ConversationMessageId newId() { return new ConversationMessageId(UUID.randomUUID()); }
    public static ConversationMessageId of(UUID value) { return new ConversationMessageId(value); }
    public static ConversationMessageId of(String value) { return new ConversationMessageId(UUID.fromString(value)); }
}
