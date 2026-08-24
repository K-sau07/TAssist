package com.tassist.infrastructure.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "conversation")
public class ConversationEntity {
    @Id private UUID id;
    @Column(name = "channel_id", nullable = false) private UUID channelId;

    @Column(name = "kind", nullable = false, columnDefinition = "conversation_kind")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM) @Enumerated(EnumType.STRING)
    private KindDb kind;

    @Column(name = "participant_a") private UUID participantA;
    @Column(name = "participant_b") private UUID participantB;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    public ConversationEntity() {}
    public UUID getId() { return id; } public void setId(UUID v) { this.id = v; }
    public UUID getChannelId() { return channelId; } public void setChannelId(UUID v) { this.channelId = v; }
    public KindDb getKind() { return kind; } public void setKind(KindDb v) { this.kind = v; }
    public UUID getParticipantA() { return participantA; } public void setParticipantA(UUID v) { this.participantA = v; }
    public UUID getParticipantB() { return participantB; } public void setParticipantB(UUID v) { this.participantB = v; }
    public Instant getCreatedAt() { return createdAt; } public void setCreatedAt(Instant v) { this.createdAt = v; }
    public Instant getUpdatedAt() { return updatedAt; } public void setUpdatedAt(Instant v) { this.updatedAt = v; }

    public enum KindDb { DM, GROUP }
}
