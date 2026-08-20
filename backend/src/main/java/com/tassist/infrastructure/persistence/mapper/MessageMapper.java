package com.tassist.infrastructure.persistence.mapper;

import com.tassist.domain.model.Citation;
import com.tassist.domain.model.Message;
import com.tassist.domain.vo.ChatId;
import com.tassist.domain.vo.ChunkId;
import com.tassist.domain.vo.FileId;
import com.tassist.domain.vo.MessageId;
import com.tassist.domain.vo.MessageRole;
import com.tassist.infrastructure.persistence.entity.MessageEntity;
import com.tassist.infrastructure.persistence.support.CitationJson;
import java.util.List;
import java.util.Optional;

public final class MessageMapper {
    private MessageMapper() {}

    public static MessageEntity toEntity(Message m) {
        MessageEntity e = new MessageEntity();
        e.setId(m.id().value());
        e.setChatId(m.chatId().value());
        e.setRole(MessageEntity.RoleDb.valueOf(m.role().name()));
        e.setContent(m.content());
        e.setCitations(m.citations().stream().map(c -> new CitationJson(
            c.fileId().value().toString(),
            c.chunkId().value().toString(),
            c.displayLabel(),
            c.snippet().orElse(null)
        )).toList());
        e.setMentionedFiles(m.mentionedFiles().stream().map(f -> f.value().toString()).toList());
        e.setCreatedAt(m.createdAt());
        return e;
    }

    public static Message toDomain(MessageEntity e) {
        List<Citation> citations = e.getCitations().stream().map(cj -> new Citation(
            FileId.of(cj.fileId()),
            ChunkId.of(cj.chunkId()),
            cj.displayLabel(),
            Optional.ofNullable(cj.snippet())
        )).toList();
        List<FileId> mentioned = e.getMentionedFiles().stream().map(FileId::of).toList();
        return new Message(
            MessageId.of(e.getId()),
            ChatId.of(e.getChatId()),
            MessageRole.valueOf(e.getRole().name()),
            e.getContent(),
            citations,
            mentioned,
            e.getCreatedAt()
        );
    }
}
