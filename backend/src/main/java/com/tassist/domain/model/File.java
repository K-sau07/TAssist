package com.tassist.domain.model;

import com.tassist.domain.vo.FileId;
import com.tassist.domain.vo.FileStatus;
import com.tassist.domain.vo.FileType;
import com.tassist.domain.vo.UserId;
import java.time.Instant;
import java.util.Optional;

/**
 * An uploaded file owned by a user (spec §8). Folder membership is via {@link FolderFile}
 * (0..N folders). Re-uploading identical bytes (same {@code (ownerId, contentHash)}) returns
 * the existing file rather than duplicating.
 */
public record File(
        FileId id,
        UserId ownerId,
        String originalFilename,
        FileType type,
        long sizeBytes,
        String storageKey,
        String contentHash,
        FileStatus status,
        Optional<String> failureReason,
        Instant createdAt,
        Instant updatedAt
) {
    public File {
        if (id == null) throw new IllegalArgumentException("File.id must not be null");
        if (ownerId == null) throw new IllegalArgumentException("File.ownerId must not be null");
        if (originalFilename == null || originalFilename.isBlank())
            throw new IllegalArgumentException("File.originalFilename must not be blank");
        if (type == null) throw new IllegalArgumentException("File.type must not be null");
        if (sizeBytes < 0) throw new IllegalArgumentException("File.sizeBytes must not be negative");
        if (storageKey == null || storageKey.isBlank())
            throw new IllegalArgumentException("File.storageKey must not be blank");
        if (contentHash == null || contentHash.isBlank())
            throw new IllegalArgumentException("File.contentHash must not be blank");
        if (status == null) throw new IllegalArgumentException("File.status must not be null");
        failureReason = failureReason == null ? Optional.empty() : failureReason;
        if (createdAt == null) throw new IllegalArgumentException("File.createdAt must not be null");
        if (updatedAt == null) throw new IllegalArgumentException("File.updatedAt must not be null");
    }
}
