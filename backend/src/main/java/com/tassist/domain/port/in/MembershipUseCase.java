package com.tassist.domain.port.in;

import com.tassist.domain.model.Membership;
import com.tassist.domain.vo.ChannelId;
import com.tassist.domain.vo.MembershipId;
import com.tassist.domain.vo.MembershipStatus;
import com.tassist.domain.vo.UserId;
import java.util.List;
import java.util.Optional;

/**
 * Inbound port: channel membership lifecycle (spec 12.5 owner side, 12.6 visitor side).
 * All transitions honour the Membership state machine; authorization verified in impl (7.4).
 */
public interface MembershipUseCase {

    Membership requestJoin(UserId actingUser, ChannelId channelId, Optional<String> message);

    /** The caller's own membership in a channel, if any (visitor-safe; no ownership required). */
    Optional<Membership> myMembership(UserId actingUser, ChannelId channelId);

    void leave(UserId actingUser, ChannelId channelId);

    List<Membership> listByStatus(UserId actingOwner, ChannelId channelId, MembershipStatus status);

    Membership approve(UserId actingOwner, ChannelId channelId, MembershipId membershipId);

    Membership deny(UserId actingOwner, ChannelId channelId, MembershipId membershipId);

    Membership kick(UserId actingOwner, ChannelId channelId, MembershipId membershipId);

    Membership ban(UserId actingOwner, ChannelId channelId, MembershipId membershipId);

    Membership reinvite(UserId actingOwner, ChannelId channelId, MembershipId membershipId, String note);
}
