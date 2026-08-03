package com.tassist.domain.model;

import com.tassist.domain.vo.UserId;
import java.time.Instant;
import java.util.UUID;

/** Dashboard note widget (spec §8). Plain text, up to ~10KB. */
public record Note(
        UUID id,
        UserId ownerId,
        String content,
        Instant createdAt,
        Instant updatedAt
) {
    public Note {
        if (id == null) throw new IllegalArgumentException("Note.id must not be null");
        if (ownerId == null) throw new IllegalArgumentException("Note.ownerId must not be null");
        if (content == null) throw new IllegalArgumentException("Note.content must not be null");
        if (createdAt == null) throw new IllegalArgumentException("Note.createdAt must not be null");
        if (updatedAt == null) throw new IllegalArgumentException("Note.updatedAt must not be null");
    }
}
