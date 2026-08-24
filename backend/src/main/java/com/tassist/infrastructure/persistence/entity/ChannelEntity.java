package com.tassist.infrastructure.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "channel")
public class ChannelEntity {
    @Id private UUID id;
    @Column(name = "owner_id", nullable = false) private UUID ownerId;
    @Column(nullable = false, unique = true) private String username;
    @Column(name = "display_name", nullable = false) private String displayName;
    @Column(nullable = false) private String description;
    @Column(name = "expectation_summary", nullable = false) private String expectationSummary;

    @Column(name = "visibility", nullable = false, columnDefinition = "channel_visibility")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM) @Enumerated(EnumType.STRING)
    private VisibilityDb visibility;

    @Column(name = "avatar_key") private String avatarKey;
    @Column(name = "require_message_on_rerequest", nullable = false) private boolean requireMessageOnReRequest;
    @Column(name = "group_chat_enabled", nullable = false) private boolean groupChatEnabled;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    public ChannelEntity() {}
    public UUID getId() { return id; } public void setId(UUID id) { this.id = id; }
    public UUID getOwnerId() { return ownerId; } public void setOwnerId(UUID o) { this.ownerId = o; }
    public String getUsername() { return username; } public void setUsername(String u) { this.username = u; }
    public String getDisplayName() { return displayName; } public void setDisplayName(String d) { this.displayName = d; }
    public String getDescription() { return description; } public void setDescription(String d) { this.description = d; }
    public String getExpectationSummary() { return expectationSummary; } public void setExpectationSummary(String e) { this.expectationSummary = e; }
    public VisibilityDb getVisibility() { return visibility; } public void setVisibility(VisibilityDb v) { this.visibility = v; }
    public String getAvatarKey() { return avatarKey; } public void setAvatarKey(String a) { this.avatarKey = a; }
    public boolean isRequireMessageOnReRequest() { return requireMessageOnReRequest; } public void setRequireMessageOnReRequest(boolean r) { this.requireMessageOnReRequest = r; }
    public boolean isGroupChatEnabled() { return groupChatEnabled; } public void setGroupChatEnabled(boolean g) { this.groupChatEnabled = g; }
    public Instant getCreatedAt() { return createdAt; } public void setCreatedAt(Instant t) { this.createdAt = t; }
    public Instant getUpdatedAt() { return updatedAt; } public void setUpdatedAt(Instant t) { this.updatedAt = t; }

    public enum VisibilityDb { PUBLIC, UNLISTED, PRIVATE }
}
