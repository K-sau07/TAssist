package com.tassist.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "note")
public class NoteEntity {
    @Id private UUID id;
    @Column(name = "owner_id", nullable = false) private UUID ownerId;
    @Column(nullable = false) private String content;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    public NoteEntity() {}
    public UUID getId() { return id; } public void setId(UUID v) { this.id = v; }
    public UUID getOwnerId() { return ownerId; } public void setOwnerId(UUID v) { this.ownerId = v; }
    public String getContent() { return content; } public void setContent(String v) { this.content = v; }
    public Instant getCreatedAt() { return createdAt; } public void setCreatedAt(Instant v) { this.createdAt = v; }
    public Instant getUpdatedAt() { return updatedAt; } public void setUpdatedAt(Instant v) { this.updatedAt = v; }
}
