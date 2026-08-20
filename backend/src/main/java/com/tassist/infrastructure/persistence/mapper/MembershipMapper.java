package com.tassist.infrastructure.persistence.mapper;

import com.tassist.domain.model.Membership;
import com.tassist.domain.vo.ChannelId;
import com.tassist.domain.vo.MembershipId;
import com.tassist.domain.vo.MembershipStatus;
import com.tassist.domain.vo.UserId;
import com.tassist.infrastructure.persistence.entity.MembershipEntity;
import java.time.Instant;
import java.util.Optional;

public final class MembershipMapper {
    private MembershipMapper() {}

    public static MembershipEntity toEntity(Membership m) {
        MembershipEntity e = new MembershipEntity();
        e.setId(m.id().value());
        e.setChannelId(m.channelId().value());
        e.setUserId(m.userId().value());
        e.setStatus(MembershipEntity.StatusDb.valueOf(m.status().name()));
        e.setRequestMessage(m.requestMessage().orElse(null));
        e.setApprovedAt(m.approvedAt().orElse(null));
        e.setRejectedAt(m.rejectedAt().orElse(null));
        e.setBannedAt(m.bannedAt().orElse(null));
        e.setLeftAt(m.leftAt().orElse(null));
        e.setCreatedAt(m.createdAt());
        e.setUpdatedAt(m.updatedAt());
        return e;
    }

    public static Membership toDomain(MembershipEntity e) {
        return new Membership(
            MembershipId.of(e.getId()),
            ChannelId.of(e.getChannelId()),
            UserId.of(e.getUserId()),
            MembershipStatus.valueOf(e.getStatus().name()),
            Optional.ofNullable(e.getRequestMessage()),
            Optional.ofNullable(e.getApprovedAt()),
            Optional.ofNullable(e.getRejectedAt()),
            Optional.ofNullable(e.getBannedAt()),
            Optional.ofNullable(e.getLeftAt()),
            e.getCreatedAt(),
            e.getUpdatedAt()
        );
    }
}
