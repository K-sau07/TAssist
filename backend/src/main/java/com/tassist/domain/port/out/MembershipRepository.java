package com.tassist.domain.port.out;

import com.tassist.domain.model.Membership;
import com.tassist.domain.vo.ChannelId;
import com.tassist.domain.vo.MembershipId;
import com.tassist.domain.vo.MembershipStatus;
import com.tassist.domain.vo.UserId;
import java.util.List;
import java.util.Optional;

/** Persistence port for {@link Membership} (owned by Channel) (spec §7). */
public interface MembershipRepository {
    Membership save(Membership membership);
    Optional<Membership> findById(MembershipId id);
    Optional<Membership> findByChannelAndUser(ChannelId channelId, UserId userId);
    List<Membership> findByChannelAndStatus(ChannelId channelId, MembershipStatus status);
    List<Membership> findByChannel(ChannelId channelId);
}
