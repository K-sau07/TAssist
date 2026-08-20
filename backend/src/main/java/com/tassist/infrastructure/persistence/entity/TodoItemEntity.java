package com.tassist.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "todo_item")
public class TodoItemEntity {
    @Id private UUID id;
    @Column(name = "owner_id", nullable = false) private UUID ownerId;
    @Column(nullable = false) private String text;
    @Column(nullable = false) private boolean done;
    @Column(nullable = false) private int position;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    public TodoItemEntity() {}
    public UUID getId() { return id; } public void setId(UUID v) { this.id = v; }
    public UUID getOwnerId() { return ownerId; } public void setOwnerId(UUID v) { this.ownerId = v; }
    public String getText() { return text; } public void setText(String v) { this.text = v; }
    public boolean isDone() { return done; } public void setDone(boolean v) { this.done = v; }
    public int getPosition() { return position; } public void setPosition(int v) { this.position = v; }
    public Instant getCreatedAt() { return createdAt; } public void setCreatedAt(Instant v) { this.createdAt = v; }
    public Instant getUpdatedAt() { return updatedAt; } public void setUpdatedAt(Instant v) { this.updatedAt = v; }
}
