package com.tassist.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "folder_file")
@IdClass(FolderFileId.class)
public class FolderFileEntity {
    @Id @Column(name = "folder_id", nullable = false) private UUID folderId;
    @Id @Column(name = "file_id", nullable = false) private UUID fileId;
    @Column(name = "added_at", nullable = false) private Instant addedAt;

    public FolderFileEntity() {}
    public UUID getFolderId() { return folderId; } public void setFolderId(UUID v) { this.folderId = v; }
    public UUID getFileId() { return fileId; } public void setFileId(UUID v) { this.fileId = v; }
    public Instant getAddedAt() { return addedAt; } public void setAddedAt(Instant v) { this.addedAt = v; }
}
