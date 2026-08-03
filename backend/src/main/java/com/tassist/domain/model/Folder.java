package com.tassist.domain.model;

import com.tassist.domain.vo.FolderId;
import com.tassist.domain.vo.UserId;
import java.time.Instant;

/** A flat (no nesting) folder owned by a user; unique {@code (ownerId, name)} (spec §8). */
public record Folder(
        FolderId id,
        UserId ownerId,
        String name,
        Instant createdAt
) {
    public Folder {
        if (id == null) throw new IllegalArgumentException("Folder.id must not be null");
        if (ownerId == null) throw new IllegalArgumentException("Folder.ownerId must not be null");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Folder.name must not be blank");
        if (createdAt == null) throw new IllegalArgumentException("Folder.createdAt must not be null");
    }
}
