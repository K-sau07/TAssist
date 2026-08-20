package com.tassist.infrastructure.persistence.repo;

import com.tassist.infrastructure.persistence.entity.ChatEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ChatJpaRepository extends JpaRepository<ChatEntity, UUID> {
    List<ChatEntity> findByOwnerIdOrderByUpdatedAtDesc(UUID ownerId);
}
