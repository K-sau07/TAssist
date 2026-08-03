package com.tassist.domain.vo;

import java.util.UUID;

/** Typed identifier for the Channel aggregate. Wraps a {@link UUID} so IDs are not interchangeable. */
public record ChannelId(UUID value) {
    public ChannelId {
        if (value == null) throw new IllegalArgumentException("ChannelId value must not be null");
    }
    public static ChannelId newId() { return new ChannelId(UUID.randomUUID()); }
    public static ChannelId of(UUID value) { return new ChannelId(value); }
    public static ChannelId of(String value) { return new ChannelId(UUID.fromString(value)); }
}
