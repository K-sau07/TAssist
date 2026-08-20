package com.tassist.infrastructure.persistence.mapper;

import com.tassist.domain.model.Chat;
import com.tassist.domain.vo.ChannelId;
import com.tassist.domain.vo.ChatId;
import com.tassist.domain.vo.ChatScope;
import com.tassist.domain.vo.FolderId;
import com.tassist.domain.vo.UserId;
import com.tassist.infrastructure.persistence.entity.ChatEntity;
import java.util.Optional;

public final class ChatMapper {
    private ChatMapper() {}

    public static ChatEntity toEntity(Chat c) {
        ChatEntity e = new ChatEntity();
        e.setId(c.id().value());
        e.setOwnerId(c.ownerId().value());
        e.setScope(ChatEntity.ScopeDb.valueOf(c.scope().name()));
        e.setFolderId(c.folderId().map(FolderId::value).orElse(null));
        e.setChannelId(c.channelId().map(ChannelId::value).orElse(null));
        e.setTitle(c.title());
        e.setCreatedAt(c.createdAt());
        e.setUpdatedAt(c.updatedAt());
        return e;
    }

    public static Chat toDomain(ChatEntity e) {
        return new Chat(
            ChatId.of(e.getId()),
            UserId.of(e.getOwnerId()),
            ChatScope.valueOf(e.getScope().name()),
            Optional.ofNullable(e.getFolderId()).map(FolderId::of),
            Optional.ofNullable(e.getChannelId()).map(ChannelId::of),
            e.getTitle(),
            e.getCreatedAt(),
            e.getUpdatedAt()
        );
    }
}
