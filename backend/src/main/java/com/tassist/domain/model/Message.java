package com.tassist.domain.model;

import com.tassist.domain.vo.ChatId;
import com.tassist.domain.vo.FileId;
import com.tassist.domain.vo.MessageId;
import com.tassist.domain.vo.MessageRole;
import java.time.Instant;
import java.util.List;

/**
 * A single message in a chat (spec §8). Citations are empty for user messages;
 * {@code mentionedFiles} carries @mentions and is only populated on user messages.
 */
public record Message(
        MessageId id,
        ChatId chatId,
        MessageRole role,
        String content,
        List<Citation> citations,
        List<FileId> mentionedFiles,
        Instant createdAt
) {
    public Message {
        if (id == null) throw new IllegalArgumentException("Message.id must not be null");
        if (chatId == null) throw new IllegalArgumentException("Message.chatId must not be null");
        if (role == null) throw new IllegalArgumentException("Message.role must not be null");
        if (content == null) throw new IllegalArgumentException("Message.content must not be null");
        citations = citations == null ? List.of() : List.copyOf(citations);
        mentionedFiles = mentionedFiles == null ? List.of() : List.copyOf(mentionedFiles);
        if (createdAt == null) throw new IllegalArgumentException("Message.createdAt must not be null");
        if (role != MessageRole.ASSISTANT && !citations.isEmpty())
            throw new IllegalArgumentException("Only ASSISTANT messages may carry citations");
        if (role != MessageRole.USER && !mentionedFiles.isEmpty())
            throw new IllegalArgumentException("Only USER messages may carry mentionedFiles");
    }
}
