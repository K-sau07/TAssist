package com.tassist.application.channel;

import com.tassist.domain.error.ConflictError;
import com.tassist.domain.error.Forbidden;
import com.tassist.domain.error.NotFoundError;
import com.tassist.domain.error.ValidationError;
import com.tassist.domain.model.Channel;
import com.tassist.domain.model.Membership;
import com.tassist.domain.port.in.MembershipUseCase;
import com.tassist.domain.port.out.ChannelRepository;
import com.tassist.domain.port.out.MembershipRepository;
import com.tassist.domain.vo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Channel membership lifecycle (§12.5 owner side, §12.6 visitor side). Every transition goes through
 * the Membership state machine (§8); owner actions require channel ownership (§7.4).
 */
@Service
public class MembershipService implements MembershipUseCase {

    private static final Logger log = LoggerFactory.getLogger(MembershipService.class);

    private final MembershipRepository memberships;
    private final ChannelRepository channels;

    public MembershipService(MembershipRepository memberships, ChannelRepository channels) {
        this.memberships = memberships;
        this.channels = channels;
    }

    // ---- visitor side (§12.6) ----

    @Override
    @Transactional
    public Membership requestJoin(UserId actingUser, ChannelId channelId, Optional<String> message) {
        Channel channel = channel(channelId);
        if (channel.ownerId().equals(actingUser))
            throw new ValidationError("owners are implicitly members of their own channel");

        Optional<Membership> existing = memberships.findByChannelAndUser(channelId, actingUser);
        Instant now = Instant.now();

        if (existing.isEmpty()) {
            Membership m = new Membership(MembershipId.newId(), channelId, actingUser,
                MembershipStatus.PENDING, message, Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), now, now);
            log.info("Join requested: channel={} user={}", channelId.value(), actingUser.value());
            return memberships.save(m);
        }

        Membership m = existing.get();
        switch (m.status()) {
            case PENDING -> throw new ConflictError("a join request is already pending");
            case APPROVED -> throw new ConflictError("already a member");
            case BANNED -> throw new Forbidden("you are banned from this channel");
            case REJECTED, LEFT -> {
                // re-request: message required if the channel demands it
                if (channel.requireMessageOnReRequest() && (message.isEmpty() || message.get().isBlank()))
                    throw new ValidationError("a message is required to request access again");
                if (!m.canTransitionTo(MembershipStatus.PENDING))
                    throw new ConflictError("cannot re-request from state " + m.status());
                Membership updated = new Membership(m.id(), channelId, actingUser, MembershipStatus.PENDING,
                    message, m.approvedAt(), m.rejectedAt(), m.bannedAt(), m.leftAt(), m.createdAt(), now);
                log.info("Re-request: channel={} user={} from={}", channelId.value(), actingUser.value(), m.status());
                return memberships.save(updated);
            }
        }
        throw new IllegalStateException("unreachable");
    }

    @Override
    @Transactional
    public void leave(UserId actingUser, ChannelId channelId) {
        Membership m = memberships.findByChannelAndUser(channelId, actingUser)
            .orElseThrow(() -> new NotFoundError("you are not a member"));
        if (!m.canTransitionTo(MembershipStatus.LEFT))
            throw new ConflictError("cannot leave from state " + m.status());
        memberships.save(transition(m, MembershipStatus.LEFT));
        log.info("Left channel: channel={} user={}", channelId.value(), actingUser.value());
    }

    // ---- owner side (§12.5) ----

    @Override
    public List<Membership> listByStatus(UserId actingOwner, ChannelId channelId, MembershipStatus status) {
        requireOwner(actingOwner, channelId);
        return memberships.findByChannelAndStatus(channelId, status);
    }

    @Override
    @Transactional
    public Membership approve(UserId actingOwner, ChannelId channelId, MembershipId membershipId) {
        return ownerTransition(actingOwner, channelId, membershipId, MembershipStatus.APPROVED);
    }

    @Override
    @Transactional
    public Membership deny(UserId actingOwner, ChannelId channelId, MembershipId membershipId) {
        return ownerTransition(actingOwner, channelId, membershipId, MembershipStatus.REJECTED);
    }

    @Override
    @Transactional
    public Membership kick(UserId actingOwner, ChannelId channelId, MembershipId membershipId) {
        // kick = remove an approved member; modelled as LEFT (they can re-request later)
        return ownerTransition(actingOwner, channelId, membershipId, MembershipStatus.LEFT);
    }

    @Override
    @Transactional
    public Membership ban(UserId actingOwner, ChannelId channelId, MembershipId membershipId) {
        return ownerTransition(actingOwner, channelId, membershipId, MembershipStatus.BANNED);
    }

    @Override
    @Transactional
    public Membership reinvite(UserId actingOwner, ChannelId channelId, MembershipId membershipId, String note) {
        Channel channel = requireOwner(actingOwner, channelId);
        Membership m = membership(channelId, membershipId);
        if (m.status() != MembershipStatus.BANNED)
            throw new ConflictError("reinvite only applies to BANNED members (was " + m.status() + ")");
        // BANNED is terminal in the base machine; reinvite is an explicit owner override → PENDING.
        Instant now = Instant.now();
        Membership updated = new Membership(m.id(), channelId, m.userId(), MembershipStatus.PENDING,
            Optional.ofNullable(note), m.approvedAt(), m.rejectedAt(), m.bannedAt(), m.leftAt(),
            m.createdAt(), now);
        log.info("Reinvited (BANNED→PENDING): channel={} member={}", channelId.value(), membershipId.value());
        return memberships.save(updated);
    }

    // ---- helpers ----

    private Membership ownerTransition(UserId actingOwner, ChannelId channelId, MembershipId membershipId,
                                       MembershipStatus target) {
        requireOwner(actingOwner, channelId);
        Membership m = membership(channelId, membershipId);
        if (!m.canTransitionTo(target))
            throw new ConflictError("illegal transition " + m.status() + " → " + target);
        Membership saved = memberships.save(transition(m, target));
        log.info("Membership {} {} → {}", membershipId.value(), m.status(), target);
        return saved;
    }

    private Membership transition(Membership m, MembershipStatus target) {
        Instant now = Instant.now();
        return new Membership(m.id(), m.channelId(), m.userId(), target,
            m.requestMessage(),
            target == MembershipStatus.APPROVED ? Optional.of(now) : m.approvedAt(),
            target == MembershipStatus.REJECTED ? Optional.of(now) : m.rejectedAt(),
            target == MembershipStatus.BANNED ? Optional.of(now) : m.bannedAt(),
            target == MembershipStatus.LEFT ? Optional.of(now) : m.leftAt(),
            m.createdAt(), now);
    }

    private Channel channel(ChannelId channelId) {
        return channels.findById(channelId).orElseThrow(() -> new NotFoundError("channel not found"));
    }

    private Channel requireOwner(UserId actingOwner, ChannelId channelId) {
        Channel c = channel(channelId);
        if (!c.ownerId().equals(actingOwner)) throw new Forbidden("not your channel");
        return c;
    }

    private Membership membership(ChannelId channelId, MembershipId membershipId) {
        Membership m = memberships.findById(membershipId)
            .orElseThrow(() -> new NotFoundError("membership not found"));
        if (!m.channelId().equals(channelId))
            throw new ValidationError("membership does not belong to this channel");
        return m;
    }
}
