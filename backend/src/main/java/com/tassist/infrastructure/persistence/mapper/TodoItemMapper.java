package com.tassist.infrastructure.persistence.mapper;

import com.tassist.domain.model.TodoItem;
import com.tassist.domain.vo.UserId;
import com.tassist.infrastructure.persistence.entity.TodoItemEntity;

public final class TodoItemMapper {
    private TodoItemMapper() {}

    public static TodoItemEntity toEntity(TodoItem t) {
        TodoItemEntity e = new TodoItemEntity();
        e.setId(t.id());
        e.setOwnerId(t.ownerId().value());
        e.setText(t.text());
        e.setDone(t.done());
        e.setPosition(t.position());
        e.setCreatedAt(t.createdAt());
        e.setUpdatedAt(t.updatedAt());
        return e;
    }

    public static TodoItem toDomain(TodoItemEntity e) {
        return new TodoItem(e.getId(), UserId.of(e.getOwnerId()), e.getText(), e.isDone(), e.getPosition(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
