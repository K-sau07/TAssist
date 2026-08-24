package com.tassist.infrastructure.persistence.adapter;

import com.tassist.domain.model.ConversationMessage;
import com.tassist.domain.port.out.ConversationMessageRepository;
import com.tassist.domain.vo.ConversationId;
import com.tassist.domain.vo.ConversationMessageId;
import com.tassist.domain.vo.UserId;
import com.tassist.infrastructure.persistence.entity.ConversationMessageEntity;
import com.tassist.infrastructure.persistence.mapper.ConversationMessageMapper;
import com.tassist.infrastructure.persistence.repo.ConversationMessageJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class ConversationMessageRepositoryAdapter implements ConversationMessageRepository {
    private final ConversationMessageJpaRepository jpa;
    public ConversationMessageRepositoryAdapter(ConversationMessageJpaRepository jpa) { this.jpa = jpa; }

    @Override public ConversationMessage save(ConversationMessage m) {
        return ConversationMessageMapper.toDomain(jpa.save(ConversationMessageMapper.toEntity(m)));
    }

    @Override public Optional<ConversationMessage> findById(ConversationMessageId id) {
        return jpa.findById(id.value()).map(ConversationMessageMapper::toDomain);
    }

    @Override public List<ConversationMessage> findByConversation(ConversationId conversationId, Instant before, int limit) {
        var page = PageRequest.of(0, Math.max(1, limit));
        List<ConversationMessageEntity> rows = (before == null)
            ? jpa.findLatestPage(conversationId.value(), page)
            : jpa.findBefore(conversationId.value(), before, page);
        // rows come newest-first; return oldest-first for rendering
        List<ConversationMessage> out = new ArrayList<>(rows.stream().map(ConversationMessageMapper::toDomain).toList());
        java.util.Collections.reverse(out);
        return out;
    }

    @Override public Optional<ConversationMessage> findLatest(ConversationId conversationId) {
        var page = PageRequest.of(0, 1);
        return jpa.findLatestVisible(conversationId.value(), page)
            .stream().findFirst().map(ConversationMessageMapper::toDomain);
    }

    @Override public long countUnread(ConversationId conversationId, Instant after, UserId excludeSender) {
        return jpa.countUnread(conversationId.value(), after, excludeSender.value());
    }
}
