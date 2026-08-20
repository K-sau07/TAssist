package com.tassist.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "folder")
public class FolderEntity {
    @Id private UUID id;
    @Column(name = "owner_id", nullable = false) private UUID ownerId;
    @Column(nullable = false) private String name;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    public FolderEntity() {}
    public UUID getId() { return id; } public void setId(UUID id) { this.id = id; }
    public UUID getOwnerId() { return ownerId; } public void setOwnerId(UUID o) { this.ownerId = o; }
    public String getName() { return name; } public void setName(String n) { this.name = n; }
    public Instant getCreatedAt() { return createdAt; } public void setCreatedAt(Instant t) { this.createdAt = t; }
    public Instant getUpdatedAt() { return updatedAt; } public void setUpdatedAt(Instant t) { this.updatedAt = t; }
}
