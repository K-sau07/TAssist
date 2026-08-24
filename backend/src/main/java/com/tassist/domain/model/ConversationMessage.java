package com.tassist.domain.model;

import com.tassist.domain.vo.ConversationId;
import com.tassist.domain.vo.ConversationMessageId;
import com.tassist.domain.vo.MessageSenderKind;
import com.tassist.domain.vo.UserId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * A single message in a human conversation (02_MESSAGING_SPEC §5).
 *
 * <p>Invariants (enforced here):
 * <ul>
 *   <li>{@code HUMAN} ⇒ {@code senderId} present (the human author).</li>
 *   <li>{@code AI}    ⇒ {@code senderId} empty; citations use {@code displayLabel} only (RAG §7.5).</li>
 *   <li>Only {@code AI} messages may carry citations.</li>
 * </ul>
 *
 * <p>Deletion is soft: {@code deletedAt} present renders a tombstone rather than removing the row,
 * preserving thread ordering and moderation audit.
 */
public record ConversationMessage(
        ConversationMessageId id,
        ConversationId conversationId,
        MessageSenderKind senderKind,
        Optional<UserId> senderId,
        String content,
        List<Citation> citations,
        Instant createdAt,
        Optional<Instant> deletedAt
) {
    public ConversationMessage {
        if (id == null) throw new IllegalArgumentException("ConversationMessage.id must not be null");
        if (conversationId == null) throw new IllegalArgumentException("ConversationMessage.conversationId must not be null");
        if (senderKind == null) throw new IllegalArgumentException("ConversationMessage.senderKind must not be null");
        senderId = senderId == null ? Optional.empty() : senderId;
        if (content == null) throw new IllegalArgumentException("ConversationMessage.content must not be null");
        citations = citations == null ? List.of() : List.copyOf(citations);
        if (createdAt == null) throw new IllegalArgumentException("ConversationMessage.createdAt must not be null");
        deletedAt = deletedAt == null ? Optional.empty() : deletedAt;

        switch (senderKind) {
            case HUMAN -> {
                if (senderId.isEmpty())
                    throw new IllegalArgumentException("HUMAN message must have a senderId");
                if (!citations.isEmpty())
                    throw new IllegalArgumentException("HUMAN message must not carry citations");
            }
            case AI -> {
                if (senderId.isPresent())
                    throw new IllegalArgumentException("AI message must not have a senderId");
            }
        }
    }

    /** A human-authored message. */
    public static ConversationMessage human(ConversationMessageId id, ConversationId convId,
                                            UserId sender, String content, Instant now) {
        return new ConversationMessage(id, convId, MessageSenderKind.HUMAN, Optional.of(sender),
            content, List.of(), now, Optional.empty());
    }

    /** An AI-authored (grounded) message with citations. */
    public static ConversationMessage ai(ConversationMessageId id, ConversationId convId,
                                         String content, List<Citation> citations, Instant now) {
        return new ConversationMessage(id, convId, MessageSenderKind.AI, Optional.empty(),
            content, citations, now, Optional.empty());
    }

    public boolean isDeleted() { return deletedAt.isPresent(); }

    /** Returns a soft-deleted copy (tombstone) at {@code now}. Idempotent. */
    public ConversationMessage deleted(Instant now) {
        if (isDeleted()) return this;
        return new ConversationMessage(id, conversationId, senderKind, senderId,
            content, citations, createdAt, Optional.of(now));
    }
}
