package com.tassist.infrastructure.persistence.repo;

import com.tassist.infrastructure.persistence.entity.ConversationReadEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ConversationReadJpaRepository
        extends JpaRepository<ConversationReadEntity, ConversationReadEntity.Key> {
    Optional<ConversationReadEntity> findByConversationIdAndUserId(java.util.UUID conversationId, java.util.UUID userId);
}
