package com.tassist.domain.model;

import com.tassist.domain.vo.UserId;
import java.time.Instant;
import java.util.UUID;

/** Dashboard to-do widget item (spec §8). {@code position} is used for reordering. */
public record TodoItem(
        UUID id,
        UserId ownerId,
        String text,
        boolean done,
        int position,
        Instant createdAt,
        Instant updatedAt
) {
    public TodoItem {
        if (id == null) throw new IllegalArgumentException("TodoItem.id must not be null");
        if (ownerId == null) throw new IllegalArgumentException("TodoItem.ownerId must not be null");
        if (text == null || text.isBlank()) throw new IllegalArgumentException("TodoItem.text must not be blank");
        if (position < 0) throw new IllegalArgumentException("TodoItem.position must not be negative");
        if (createdAt == null) throw new IllegalArgumentException("TodoItem.createdAt must not be null");
        if (updatedAt == null) throw new IllegalArgumentException("TodoItem.updatedAt must not be null");
    }
}
