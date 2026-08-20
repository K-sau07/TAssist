package com.tassist.infrastructure.persistence.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class ChannelFileId implements Serializable {
    private UUID channelId;
    private UUID fileId;
    public ChannelFileId() {}
    public ChannelFileId(UUID channelId, UUID fileId) { this.channelId = channelId; this.fileId = fileId; }
    public UUID getChannelId() { return channelId; } public void setChannelId(UUID v) { this.channelId = v; }
    public UUID getFileId() { return fileId; } public void setFileId(UUID v) { this.fileId = v; }
    @Override public boolean equals(Object o) {
        if (this == o) return true; if (!(o instanceof ChannelFileId t)) return false;
        return Objects.equals(channelId, t.channelId) && Objects.equals(fileId, t.fileId);
    }
    @Override public int hashCode() { return Objects.hash(channelId, fileId); }
}
