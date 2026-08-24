package com.tassist.infrastructure.persistence.mapper;

import com.tassist.domain.model.Citation;
import com.tassist.domain.model.ConversationMessage;
import com.tassist.domain.vo.ChunkId;
import com.tassist.domain.vo.ConversationId;
import com.tassist.domain.vo.ConversationMessageId;
import com.tassist.domain.vo.FileId;
import com.tassist.domain.vo.MessageSenderKind;
import com.tassist.domain.vo.UserId;
import com.tassist.infrastructure.persistence.entity.ConversationMessageEntity;
import com.tassist.infrastructure.persistence.support.CitationJson;
import java.util.List;
import java.util.Optional;

public final class ConversationMessageMapper {
    private ConversationMessageMapper() {}

    public static ConversationMessageEntity toEntity(ConversationMessage m) {
        ConversationMessageEntity e = new ConversationMessageEntity();
        e.setId(m.id().value());
        e.setConversationId(m.conversationId().value());
        e.setSenderKind(ConversationMessageEntity.SenderKindDb.valueOf(m.senderKind().name()));
        e.setSenderId(m.senderId().map(UserId::value).orElse(null));
        e.setContent(m.content());
        e.setCitations(m.citations().stream().map(c -> new CitationJson(
            c.fileId().value().toString(),
            c.chunkId().value().toString(),
            c.displayLabel(),
            c.snippet().orElse(null)
        )).toList());
        e.setCreatedAt(m.createdAt());
        e.setDeletedAt(m.deletedAt().orElse(null));
        return e;
    }

    public static ConversationMessage toDomain(ConversationMessageEntity e) {
        List<Citation> citations = e.getCitations().stream().map(cj -> new Citation(
            FileId.of(cj.fileId()),
            ChunkId.of(cj.chunkId()),
            cj.displayLabel(),
            Optional.ofNullable(cj.snippet())
        )).toList();
        return new ConversationMessage(
            ConversationMessageId.of(e.getId()),
            ConversationId.of(e.getConversationId()),
            MessageSenderKind.valueOf(e.getSenderKind().name()),
            Optional.ofNullable(e.getSenderId()).map(UserId::of),
            e.getContent(),
            citations,
            e.getCreatedAt(),
            Optional.ofNullable(e.getDeletedAt())
        );
    }
}
