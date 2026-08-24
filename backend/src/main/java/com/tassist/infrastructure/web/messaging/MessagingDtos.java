package com.tassist.infrastructure.web.messaging;

import com.tassist.domain.model.Citation;
import com.tassist.domain.model.Conversation;
import com.tassist.domain.model.ConversationMessage;
import com.tassist.domain.model.User;

import java.time.Instant;
import java.util.List;

/** Request/response DTOs for channel messaging endpoints (02_MESSAGING_SPEC §9). */
public final class MessagingDtos {
    private MessagingDtos() {}

    // ── requests ──
    public record OpenDmRequest(String targetUserId) {}
    public record PostMessageRequest(String content) {}
    public record MarkReadRequest(Instant upTo) {}
    public record GroupEnabledRequest(Boolean enabled) {}

    // ── views ──
    public record ParticipantView(String userId, String displayName, boolean isOwner) {
        public static ParticipantView of(User u, boolean isOwner) {
            return new ParticipantView(u.id().value().toString(), u.displayName(), isOwner);
        }
    }

    /** A conversation summary for the inbox. otherParticipant is null for GROUP. */
    public record ConversationView(String id, String channelId, String kind,
                                   ParticipantView otherParticipant,
                                   String lastMessagePreview, long unreadCount, Instant updatedAt) {}

    public record MessageSender(String userId, String displayName) {}

    public record CitationView(String fileId, String chunkId, String displayLabel, String snippet) {
        public static CitationView of(Citation c) {
            return new CitationView(c.fileId().value().toString(), c.chunkId().value().toString(),
                c.displayLabel(), c.snippet().orElse(null));
        }
    }

    /** A rendered message. Deleted messages return a tombstone (content nulled). */
    public record MessageView(String id, String senderKind, MessageSender sender,
                              String content, List<CitationView> citations,
                              Instant createdAt, boolean deleted) {
        public static MessageView of(ConversationMessage m, MessageSender sender) {
            boolean deleted = m.isDeleted();
            return new MessageView(
                m.id().value().toString(),
                m.senderKind().name(),
                sender,
                deleted ? null : m.content(),
                deleted ? List.of() : m.citations().stream().map(CitationView::of).toList(),
                m.createdAt(),
                deleted);
        }
    }

    /** Result of posting a message: the human message, plus the AI reply if @ai/@assist was tagged. */
    public record PostMessageResponse(MessageView message, MessageView aiReply) {}
}
