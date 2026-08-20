package com.tassist.infrastructure.persistence.repo;

import com.tassist.infrastructure.persistence.entity.TodoItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface TodoItemJpaRepository extends JpaRepository<TodoItemEntity, UUID> {
    List<TodoItemEntity> findByOwnerIdOrderByPositionAsc(UUID ownerId);
}
