package com.tassist.domain.model;

import com.tassist.domain.vo.ChannelId;
import com.tassist.domain.vo.ConversationId;
import com.tassist.domain.vo.ConversationKind;
import com.tassist.domain.vo.UserId;
import java.time.Instant;
import java.util.Optional;

/**
 * A human conversation inside a channel (02_MESSAGING_SPEC §5).
 *
 * <p>Structural invariants (enforced here):
 * <ul>
 *   <li>{@code DM}    ⇒ both participants present, distinct, and stored in canonical order
 *       (participantA's UUID &lt; participantB's UUID) so a pair maps to one conversation
 *       regardless of who opened it.</li>
 *   <li>{@code GROUP} ⇒ both participants empty (membership defines who can post).</li>
 * </ul>
 *
 * <p>Behavioural rules that need external state — that both participants are approved members
 * (or owner), and that the group room is enabled — are enforced in the application layer
 * (02_MESSAGING_SPEC §6), not here.
 */
public record Conversation(
        ConversationId id,
        ChannelId channelId,
        ConversationKind kind,
        Optional<UserId> participantA,
        Optional<UserId> participantB,
        Instant createdAt,
        Instant updatedAt
) {
    public Conversation {
        if (id == null) throw new IllegalArgumentException("Conversation.id must not be null");
        if (channelId == null) throw new IllegalArgumentException("Conversation.channelId must not be null");
        if (kind == null) throw new IllegalArgumentException("Conversation.kind must not be null");
        participantA = participantA == null ? Optional.empty() : participantA;
        participantB = participantB == null ? Optional.empty() : participantB;
        if (createdAt == null) throw new IllegalArgumentException("Conversation.createdAt must not be null");
        if (updatedAt == null) throw new IllegalArgumentException("Conversation.updatedAt must not be null");

        switch (kind) {
            case DM -> {
                if (participantA.isEmpty() || participantB.isEmpty())
                    throw new IllegalArgumentException("DM conversation must have two participants");
                if (participantA.get().equals(participantB.get()))
                    throw new IllegalArgumentException("DM participants must be distinct");
                // canonical order matches Postgres uuid ordering (unsigned, byte-wise), not Java's signed UUID.compareTo
                if (compareUnsigned(participantA.get().value(), participantB.get().value()) >= 0)
                    throw new IllegalArgumentException(
                        "DM participants must be in canonical order (participantA < participantB); use Conversation.dm(...)");
            }
            case GROUP -> {
                if (participantA.isPresent() || participantB.isPresent())
                    throw new IllegalArgumentException("GROUP conversation must have no participants");
            }
        }
    }

    /** Create a DM with participants normalised into canonical order (smaller UUID first). */
    public static Conversation dm(ConversationId id, ChannelId channelId, UserId x, UserId y, Instant now) {
        if (x == null || y == null) throw new IllegalArgumentException("DM participants must not be null");
        if (x.equals(y)) throw new IllegalArgumentException("DM participants must be distinct");
        UserId a = compareUnsigned(x.value(), y.value()) < 0 ? x : y;
        UserId b = a.equals(x) ? y : x;
        return new Conversation(id, channelId, ConversationKind.DM, Optional.of(a), Optional.of(b), now, now);
    }

    /** Create the channel's group room. */
    public static Conversation group(ConversationId id, ChannelId channelId, Instant now) {
        return new Conversation(id, channelId, ConversationKind.GROUP, Optional.empty(), Optional.empty(), now, now);
    }

    /** True if the given user is one of this DM's two participants. Always false for GROUP. */
    public boolean hasParticipant(UserId user) {
        return participantA.map(p -> p.equals(user)).orElse(false)
            || participantB.map(p -> p.equals(user)).orElse(false);
    }

    /** The other party in a DM relative to {@code me}, if {@code me} is a participant. */
    public Optional<UserId> otherParticipant(UserId me) {
        if (kind != ConversationKind.DM || !hasParticipant(me)) return Optional.empty();
        return participantA.get().equals(me) ? participantB : participantA;
    }

    /** Returns a copy with updatedAt bumped to {@code now} (called when a new message arrives). */
    public Conversation touched(Instant now) {
        return new Conversation(id, channelId, kind, participantA, participantB, createdAt, now);
    }

    /**
     * Compare two UUIDs the way PostgreSQL orders its {@code uuid} type: as 16 unsigned bytes.
     * Java's {@link java.util.UUID#compareTo} is signed on each long half, which disagrees with the DB
     * for high-bit UUIDs and would let a pair map to two different canonical orders (duplicate DMs).
     */
    static int compareUnsigned(java.util.UUID x, java.util.UUID y) {
        int hi = Long.compareUnsigned(x.getMostSignificantBits(), y.getMostSignificantBits());
        if (hi != 0) return hi;
        return Long.compareUnsigned(x.getLeastSignificantBits(), y.getLeastSignificantBits());
    }
}
