package com.tassist.infrastructure.persistence.entity;

import com.tassist.infrastructure.persistence.support.CitationJson;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "message")
public class MessageEntity {
    @Id private UUID id;
    @Column(name = "chat_id", nullable = false) private UUID chatId;

    @Column(name = "role", nullable = false, columnDefinition = "message_role")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM) @Enumerated(EnumType.STRING)
    private RoleDb role;

    @Column(nullable = false) private String content;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<CitationJson> citations;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "mentioned_files", nullable = false, columnDefinition = "jsonb")
    private List<String> mentionedFiles;

    @Column(name = "created_at", nullable = false) private Instant createdAt;

    public MessageEntity() {}
    public UUID getId() { return id; } public void setId(UUID v) { this.id = v; }
    public UUID getChatId() { return chatId; } public void setChatId(UUID v) { this.chatId = v; }
    public RoleDb getRole() { return role; } public void setRole(RoleDb v) { this.role = v; }
    public String getContent() { return content; } public void setContent(String v) { this.content = v; }
    public List<CitationJson> getCitations() { return citations; } public void setCitations(List<CitationJson> v) { this.citations = v; }
    public List<String> getMentionedFiles() { return mentionedFiles; } public void setMentionedFiles(List<String> v) { this.mentionedFiles = v; }
    public Instant getCreatedAt() { return createdAt; } public void setCreatedAt(Instant v) { this.createdAt = v; }

    public enum RoleDb { USER, ASSISTANT, SYSTEM }
}
