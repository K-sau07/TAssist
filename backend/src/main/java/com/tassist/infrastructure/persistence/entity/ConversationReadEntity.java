package com.tassist.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "conversation_read")
@IdClass(ConversationReadEntity.Key.class)
public class ConversationReadEntity {
    @Id @Column(name = "conversation_id", nullable = false) private UUID conversationId;
    @Id @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(name = "last_read_at", nullable = false) private Instant lastReadAt;

    public ConversationReadEntity() {}
    public UUID getConversationId() { return conversationId; } public void setConversationId(UUID v) { this.conversationId = v; }
    public UUID getUserId() { return userId; } public void setUserId(UUID v) { this.userId = v; }
    public Instant getLastReadAt() { return lastReadAt; } public void setLastReadAt(Instant v) { this.lastReadAt = v; }

    public static class Key implements Serializable {
        private UUID conversationId;
        private UUID userId;
        public Key() {}
        public Key(UUID conversationId, UUID userId) { this.conversationId = conversationId; this.userId = userId; }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key k)) return false;
            return Objects.equals(conversationId, k.conversationId) && Objects.equals(userId, k.userId);
        }
        @Override public int hashCode() { return Objects.hash(conversationId, userId); }
    }
}
