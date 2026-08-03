package com.tassist.domain.model;

import com.tassist.domain.vo.ChannelId;
import com.tassist.domain.vo.MembershipId;
import com.tassist.domain.vo.MembershipStatus;
import com.tassist.domain.vo.UserId;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

/**
 * A user's membership in a channel (spec §8). Unique per {@code (channelId, userId)}.
 *
 * <p>State machine (enforced by {@link #canTransitionTo}):
 * <pre>
 *   PENDING  → APPROVED | REJECTED | BANNED
 *   APPROVED → LEFT | BANNED
 *   REJECTED → PENDING        (re-request; requires a message)
 *   BANNED   → (terminal)
 *   LEFT     → PENDING        (new request)
 * </pre>
 */
public record Membership(
        MembershipId id,
        ChannelId channelId,
        UserId userId,
        MembershipStatus status,
        Optional<String> requestMessage,
        Optional<Instant> approvedAt,
        Optional<Instant> rejectedAt,
        Optional<Instant> bannedAt,
        Optional<Instant> leftAt,
        Instant createdAt,
        Instant updatedAt
) {
    public Membership {
        if (id == null) throw new IllegalArgumentException("Membership.id must not be null");
        if (channelId == null) throw new IllegalArgumentException("Membership.channelId must not be null");
        if (userId == null) throw new IllegalArgumentException("Membership.userId must not be null");
        if (status == null) throw new IllegalArgumentException("Membership.status must not be null");
        requestMessage = requestMessage == null ? Optional.empty() : requestMessage;
        approvedAt = approvedAt == null ? Optional.empty() : approvedAt;
        rejectedAt = rejectedAt == null ? Optional.empty() : rejectedAt;
        bannedAt = bannedAt == null ? Optional.empty() : bannedAt;
        leftAt = leftAt == null ? Optional.empty() : leftAt;
        if (createdAt == null) throw new IllegalArgumentException("Membership.createdAt must not be null");
        if (updatedAt == null) throw new IllegalArgumentException("Membership.updatedAt must not be null");
    }

    /** Legal target states from each current state (spec §8 state machine). */
    public boolean canTransitionTo(MembershipStatus target) {
        Set<MembershipStatus> allowed = switch (status) {
            case PENDING  -> Set.of(MembershipStatus.APPROVED, MembershipStatus.REJECTED, MembershipStatus.BANNED);
            case APPROVED -> Set.of(MembershipStatus.LEFT, MembershipStatus.BANNED);
            case REJECTED -> Set.of(MembershipStatus.PENDING);
            case LEFT     -> Set.of(MembershipStatus.PENDING);
            case BANNED   -> Set.of();
        };
        return allowed.contains(target);
    }
}
