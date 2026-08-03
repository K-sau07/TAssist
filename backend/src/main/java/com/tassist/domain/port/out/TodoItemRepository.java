package com.tassist.domain.port.out;

import com.tassist.domain.model.TodoItem;
import com.tassist.domain.vo.UserId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persistence port for {@link TodoItem} (spec §7). */
public interface TodoItemRepository {
    TodoItem save(TodoItem item);
    Optional<TodoItem> findById(UUID id);
    List<TodoItem> findByOwner(UserId ownerId);
    void delete(UUID id);
}
