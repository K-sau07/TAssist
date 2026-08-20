package com.tassist.infrastructure.persistence.mapper;

import com.tassist.domain.model.Note;
import com.tassist.domain.vo.UserId;
import com.tassist.infrastructure.persistence.entity.NoteEntity;

public final class NoteMapper {
    private NoteMapper() {}

    public static NoteEntity toEntity(Note n) {
        NoteEntity e = new NoteEntity();
        e.setId(n.id());
        e.setOwnerId(n.ownerId().value());
        e.setContent(n.content());
        e.setCreatedAt(n.createdAt());
        e.setUpdatedAt(n.updatedAt());
        return e;
    }

    public static Note toDomain(NoteEntity e) {
        return new Note(e.getId(), UserId.of(e.getOwnerId()), e.getContent(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
