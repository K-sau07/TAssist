package com.tassist.infrastructure.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chat")
public class ChatEntity {
    @Id private UUID id;
    @Column(name = "owner_id", nullable = false) private UUID ownerId;

    @Column(name = "scope", nullable = false, columnDefinition = "chat_scope")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM) @Enumerated(EnumType.STRING)
    private ScopeDb scope;

    @Column(name = "folder_id") private UUID folderId;
    @Column(name = "channel_id") private UUID channelId;
    @Column(nullable = false) private String title;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    public ChatEntity() {}
    public UUID getId() { return id; } public void setId(UUID v) { this.id = v; }
    public UUID getOwnerId() { return ownerId; } public void setOwnerId(UUID v) { this.ownerId = v; }
    public ScopeDb getScope() { return scope; } public void setScope(ScopeDb v) { this.scope = v; }
    public UUID getFolderId() { return folderId; } public void setFolderId(UUID v) { this.folderId = v; }
    public UUID getChannelId() { return channelId; } public void setChannelId(UUID v) { this.channelId = v; }
    public String getTitle() { return title; } public void setTitle(String v) { this.title = v; }
    public Instant getCreatedAt() { return createdAt; } public void setCreatedAt(Instant v) { this.createdAt = v; }
    public Instant getUpdatedAt() { return updatedAt; } public void setUpdatedAt(Instant v) { this.updatedAt = v; }

    public enum ScopeDb { REGULAR, FOLDER, CHANNEL }
}
