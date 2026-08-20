package com.tassist.infrastructure.persistence.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class FolderFileId implements Serializable {
    private UUID folderId;
    private UUID fileId;
    public FolderFileId() {}
    public FolderFileId(UUID folderId, UUID fileId) { this.folderId = folderId; this.fileId = fileId; }
    public UUID getFolderId() { return folderId; } public void setFolderId(UUID v) { this.folderId = v; }
    public UUID getFileId() { return fileId; } public void setFileId(UUID v) { this.fileId = v; }
    @Override public boolean equals(Object o) {
        if (this == o) return true; if (!(o instanceof FolderFileId t)) return false;
        return Objects.equals(folderId, t.folderId) && Objects.equals(fileId, t.fileId);
    }
    @Override public int hashCode() { return Objects.hash(folderId, fileId); }
}
