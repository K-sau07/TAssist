package com.tassist.infrastructure.persistence.entity;

import com.tassist.infrastructure.persistence.support.CitationJson;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "conversation_message")
public class ConversationMessageEntity {
    @Id private UUID id;
    @Column(name = "conversation_id", nullable = false) private UUID conversationId;

    @Column(name = "sender_kind", nullable = false, columnDefinition = "msg_sender_kind")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM) @Enumerated(EnumType.STRING)
    private SenderKindDb senderKind;

    @Column(name = "sender_id") private UUID senderId;
    @Column(nullable = false) private String content;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<CitationJson> citations;

    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "deleted_at") private Instant deletedAt;

    public ConversationMessageEntity() {}
    public UUID getId() { return id; } public void setId(UUID v) { this.id = v; }
    public UUID getConversationId() { return conversationId; } public void setConversationId(UUID v) { this.conversationId = v; }
    public SenderKindDb getSenderKind() { return senderKind; } public void setSenderKind(SenderKindDb v) { this.senderKind = v; }
    public UUID getSenderId() { return senderId; } public void setSenderId(UUID v) { this.senderId = v; }
    public String getContent() { return content; } public void setContent(String v) { this.content = v; }
    public List<CitationJson> getCitations() { return citations; } public void setCitations(List<CitationJson> v) { this.citations = v; }
    public Instant getCreatedAt() { return createdAt; } public void setCreatedAt(Instant v) { this.createdAt = v; }
    public Instant getDeletedAt() { return deletedAt; } public void setDeletedAt(Instant v) { this.deletedAt = v; }

    public enum SenderKindDb { HUMAN, AI }
}
