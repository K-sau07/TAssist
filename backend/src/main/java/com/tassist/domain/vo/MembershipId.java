package com.tassist.domain.vo;

import java.util.UUID;

/** Typed identifier for the Membership aggregate. Wraps a {@link UUID} so IDs are not interchangeable. */
public record MembershipId(UUID value) {
    public MembershipId {
        if (value == null) throw new IllegalArgumentException("MembershipId value must not be null");
    }
    public static MembershipId newId() { return new MembershipId(UUID.randomUUID()); }
    public static MembershipId of(UUID value) { return new MembershipId(value); }
    public static MembershipId of(String value) { return new MembershipId(UUID.fromString(value)); }
}
