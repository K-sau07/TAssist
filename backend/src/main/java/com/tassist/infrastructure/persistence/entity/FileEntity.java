package com.tassist.infrastructure.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "file")
public class FileEntity {
    @Id private UUID id;
    @Column(name = "owner_id", nullable = false) private UUID ownerId;
    @Column(name = "original_filename", nullable = false) private String originalFilename;

    @Column(name = "type", nullable = false, columnDefinition = "file_type")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM) @Enumerated(EnumType.STRING)
    private FileTypeDb type;

    @Column(name = "size_bytes", nullable = false) private long sizeBytes;
    @Column(name = "storage_key", nullable = false) private String storageKey;
    @Column(name = "content_hash", nullable = false) private String contentHash;

    @Column(name = "status", nullable = false, columnDefinition = "file_status")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM) @Enumerated(EnumType.STRING)
    private FileStatusDb status;

    @Column(name = "failure_reason") private String failureReason;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    public FileEntity() {}
    public UUID getId() { return id; } public void setId(UUID id) { this.id = id; }
    public UUID getOwnerId() { return ownerId; } public void setOwnerId(UUID o) { this.ownerId = o; }
    public String getOriginalFilename() { return originalFilename; } public void setOriginalFilename(String f) { this.originalFilename = f; }
    public FileTypeDb getType() { return type; } public void setType(FileTypeDb t) { this.type = t; }
    public long getSizeBytes() { return sizeBytes; } public void setSizeBytes(long s) { this.sizeBytes = s; }
    public String getStorageKey() { return storageKey; } public void setStorageKey(String k) { this.storageKey = k; }
    public String getContentHash() { return contentHash; } public void setContentHash(String h) { this.contentHash = h; }
    public FileStatusDb getStatus() { return status; } public void setStatus(FileStatusDb s) { this.status = s; }
    public String getFailureReason() { return failureReason; } public void setFailureReason(String r) { this.failureReason = r; }
    public Instant getCreatedAt() { return createdAt; } public void setCreatedAt(Instant t) { this.createdAt = t; }
    public Instant getUpdatedAt() { return updatedAt; } public void setUpdatedAt(Instant t) { this.updatedAt = t; }

    public enum FileTypeDb { PDF, DOCX, PPTX, XLSX, CSV, TXT, MD }
    public enum FileStatusDb { UPLOADING, PARSING, EMBEDDING, READY, FAILED }
}
