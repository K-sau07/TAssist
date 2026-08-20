package com.tassist.infrastructure.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "membership")
public class MembershipEntity {
    @Id private UUID id;
    @Column(name = "channel_id", nullable = false) private UUID channelId;
    @Column(name = "user_id", nullable = false) private UUID userId;

    @Column(name = "status", nullable = false, columnDefinition = "membership_status")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM) @Enumerated(EnumType.STRING)
    private StatusDb status;

    @Column(name = "request_message") private String requestMessage;
    @Column(name = "approved_at") private Instant approvedAt;
    @Column(name = "rejected_at") private Instant rejectedAt;
    @Column(name = "banned_at") private Instant bannedAt;
    @Column(name = "left_at") private Instant leftAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    public MembershipEntity() {}
    public UUID getId() { return id; } public void setId(UUID v) { this.id = v; }
    public UUID getChannelId() { return channelId; } public void setChannelId(UUID v) { this.channelId = v; }
    public UUID getUserId() { return userId; } public void setUserId(UUID v) { this.userId = v; }
    public StatusDb getStatus() { return status; } public void setStatus(StatusDb v) { this.status = v; }
    public String getRequestMessage() { return requestMessage; } public void setRequestMessage(String v) { this.requestMessage = v; }
    public Instant getApprovedAt() { return approvedAt; } public void setApprovedAt(Instant v) { this.approvedAt = v; }
    public Instant getRejectedAt() { return rejectedAt; } public void setRejectedAt(Instant v) { this.rejectedAt = v; }
    public Instant getBannedAt() { return bannedAt; } public void setBannedAt(Instant v) { this.bannedAt = v; }
    public Instant getLeftAt() { return leftAt; } public void setLeftAt(Instant v) { this.leftAt = v; }
    public Instant getCreatedAt() { return createdAt; } public void setCreatedAt(Instant v) { this.createdAt = v; }
    public Instant getUpdatedAt() { return updatedAt; } public void setUpdatedAt(Instant v) { this.updatedAt = v; }

    public enum StatusDb { PENDING, APPROVED, REJECTED, BANNED, LEFT }
}
