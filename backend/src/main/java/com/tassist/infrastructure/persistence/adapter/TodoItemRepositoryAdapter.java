package com.tassist.infrastructure.persistence.adapter;

import com.tassist.domain.model.TodoItem;
import com.tassist.domain.port.out.TodoItemRepository;
import com.tassist.domain.vo.UserId;
import com.tassist.infrastructure.persistence.mapper.TodoItemMapper;
import com.tassist.infrastructure.persistence.repo.TodoItemJpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class TodoItemRepositoryAdapter implements TodoItemRepository {
    private final TodoItemJpaRepository jpa;
    public TodoItemRepositoryAdapter(TodoItemJpaRepository jpa) { this.jpa = jpa; }

    @Override public TodoItem save(TodoItem t) { return TodoItemMapper.toDomain(jpa.save(TodoItemMapper.toEntity(t))); }
    @Override public Optional<TodoItem> findById(UUID id) { return jpa.findById(id).map(TodoItemMapper::toDomain); }
    @Override public List<TodoItem> findByOwner(UserId ownerId) { return jpa.findByOwnerIdOrderByPositionAsc(ownerId.value()).stream().map(TodoItemMapper::toDomain).toList(); }
    @Override public void delete(UUID id) { jpa.deleteById(id); }
}
