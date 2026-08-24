package com.tassist.infrastructure.persistence.mapper;

import com.tassist.domain.model.Channel;
import com.tassist.domain.vo.ChannelId;
import com.tassist.domain.vo.ChannelVisibility;
import com.tassist.domain.vo.UserId;
import com.tassist.infrastructure.persistence.entity.ChannelEntity;
import java.util.Optional;

public final class ChannelMapper {
    private ChannelMapper() {}

    public static ChannelEntity toEntity(Channel c) {
        ChannelEntity e = new ChannelEntity();
        e.setId(c.id().value());
        e.setOwnerId(c.ownerId().value());
        e.setUsername(c.username());
        e.setDisplayName(c.displayName());
        e.setDescription(c.description());
        e.setExpectationSummary(c.expectationSummary());
        e.setVisibility(ChannelEntity.VisibilityDb.valueOf(c.visibility().name()));
        e.setAvatarKey(c.avatarKey().orElse(null));
        e.setRequireMessageOnReRequest(c.requireMessageOnReRequest());
        e.setGroupChatEnabled(c.groupChatEnabled());
        e.setCreatedAt(c.createdAt());
        e.setUpdatedAt(c.updatedAt());
        return e;
    }

    public static Channel toDomain(ChannelEntity e) {
        return new Channel(
            ChannelId.of(e.getId()),
            UserId.of(e.getOwnerId()),
            e.getUsername(),
            e.getDisplayName(),
            e.getDescription(),
            e.getExpectationSummary(),
            ChannelVisibility.valueOf(e.getVisibility().name()),
            Optional.ofNullable(e.getAvatarKey()),
            e.isRequireMessageOnReRequest(),
            e.isGroupChatEnabled(),
            e.getCreatedAt(),
            e.getUpdatedAt()
        );
    }
}
