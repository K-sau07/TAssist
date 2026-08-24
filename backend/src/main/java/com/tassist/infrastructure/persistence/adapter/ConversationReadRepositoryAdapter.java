package com.tassist.infrastructure.persistence.adapter;

import com.tassist.domain.port.out.ConversationReadRepository;
import com.tassist.domain.vo.ConversationId;
import com.tassist.domain.vo.UserId;
import com.tassist.infrastructure.persistence.entity.ConversationReadEntity;
import com.tassist.infrastructure.persistence.repo.ConversationReadJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.Optional;

@Repository
public class ConversationReadRepositoryAdapter implements ConversationReadRepository {
    private final ConversationReadJpaRepository jpa;
    public ConversationReadRepositoryAdapter(ConversationReadJpaRepository jpa) { this.jpa = jpa; }

    @Override public Optional<Instant> findLastRead(ConversationId conversationId, UserId user) {
        return jpa.findByConversationIdAndUserId(conversationId.value(), user.value())
            .map(ConversationReadEntity::getLastReadAt);
    }

    @Override @Transactional public void markRead(ConversationId conversationId, UserId user, Instant at) {
        var existing = jpa.findByConversationIdAndUserId(conversationId.value(), user.value());
        ConversationReadEntity e = existing.orElseGet(() -> {
            ConversationReadEntity n = new ConversationReadEntity();
            n.setConversationId(conversationId.value());
            n.setUserId(user.value());
            return n;
        });
        // max-wins across devices: never move last_read_at backwards
        if (e.getLastReadAt() == null || at.isAfter(e.getLastReadAt())) {
            e.setLastReadAt(at);
            jpa.save(e);
        }
    }
}
