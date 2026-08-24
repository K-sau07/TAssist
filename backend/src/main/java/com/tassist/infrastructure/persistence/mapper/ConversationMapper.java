package com.tassist.infrastructure.persistence.mapper;

import com.tassist.domain.model.Conversation;
import com.tassist.domain.vo.ChannelId;
import com.tassist.domain.vo.ConversationId;
import com.tassist.domain.vo.ConversationKind;
import com.tassist.domain.vo.UserId;
import com.tassist.infrastructure.persistence.entity.ConversationEntity;
import java.util.Optional;

public final class ConversationMapper {
    private ConversationMapper() {}

    public static ConversationEntity toEntity(Conversation c) {
        ConversationEntity e = new ConversationEntity();
        e.setId(c.id().value());
        e.setChannelId(c.channelId().value());
        e.setKind(ConversationEntity.KindDb.valueOf(c.kind().name()));
        e.setParticipantA(c.participantA().map(UserId::value).orElse(null));
        e.setParticipantB(c.participantB().map(UserId::value).orElse(null));
        e.setCreatedAt(c.createdAt());
        e.setUpdatedAt(c.updatedAt());
        return e;
    }

    public static Conversation toDomain(ConversationEntity e) {
        return new Conversation(
            ConversationId.of(e.getId()),
            ChannelId.of(e.getChannelId()),
            ConversationKind.valueOf(e.getKind().name()),
            Optional.ofNullable(e.getParticipantA()).map(UserId::of),
            Optional.ofNullable(e.getParticipantB()).map(UserId::of),
            e.getCreatedAt(),
            e.getUpdatedAt()
        );
    }
}
