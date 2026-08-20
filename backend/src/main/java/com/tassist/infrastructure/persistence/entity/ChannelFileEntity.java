package com.tassist.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "channel_file")
@IdClass(ChannelFileId.class)
public class ChannelFileEntity {
    @Id @Column(name = "channel_id", nullable = false) private UUID channelId;
    @Id @Column(name = "file_id", nullable = false) private UUID fileId;
    @Column(name = "display_label", nullable = false) private String displayLabel;
    @Column(name = "added_at", nullable = false) private Instant addedAt;

    public ChannelFileEntity() {}
    public UUID getChannelId() { return channelId; } public void setChannelId(UUID v) { this.channelId = v; }
    public UUID getFileId() { return fileId; } public void setFileId(UUID v) { this.fileId = v; }
    public String getDisplayLabel() { return displayLabel; } public void setDisplayLabel(String v) { this.displayLabel = v; }
    public Instant getAddedAt() { return addedAt; } public void setAddedAt(Instant v) { this.addedAt = v; }
}
