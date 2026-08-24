package com.tassist.infrastructure.persistence.adapter;

import com.tassist.domain.model.Conversation;
import com.tassist.domain.port.out.ConversationRepository;
import com.tassist.domain.vo.ChannelId;
import com.tassist.domain.vo.ConversationId;
import com.tassist.domain.vo.UserId;
import com.tassist.infrastructure.persistence.mapper.ConversationMapper;
import com.tassist.infrastructure.persistence.repo.ConversationJpaRepository;
import com.tassist.infrastructure.persistence.entity.ConversationEntity.KindDb;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ConversationRepositoryAdapter implements ConversationRepository {
    private final ConversationJpaRepository jpa;
    public ConversationRepositoryAdapter(ConversationJpaRepository jpa) { this.jpa = jpa; }

    @Override public Conversation save(Conversation c) {
        return ConversationMapper.toDomain(jpa.save(ConversationMapper.toEntity(c)));
    }

    @Override public Optional<Conversation> findById(ConversationId id) {
        return jpa.findById(id.value()).map(ConversationMapper::toDomain);
    }

    @Override public Optional<Conversation> findDm(ChannelId channelId, UserId x, UserId y) {
        // canonicalise the same way the domain does (unsigned uuid order = Postgres order)
        UUID xu = x.value(), yu = y.value();
        UUID a = compareUnsigned(xu, yu) < 0 ? xu : yu;
        UUID b = a.equals(xu) ? yu : xu;
        return jpa.findDm(KindDb.DM, channelId.value(), a, b).map(ConversationMapper::toDomain);
    }

    @Override public Optional<Conversation> findGroup(ChannelId channelId) {
        return jpa.findGroup(KindDb.GROUP, channelId.value()).map(ConversationMapper::toDomain);
    }

    @Override public List<Conversation> findDmsForUser(ChannelId channelId, UserId user) {
        return jpa.findDmsForUser(KindDb.DM, channelId.value(), user.value())
            .stream().map(ConversationMapper::toDomain).toList();
    }

    @Override public void delete(ConversationId id) { jpa.deleteById(id.value()); }

    private static int compareUnsigned(UUID x, UUID y) {
        int hi = Long.compareUnsigned(x.getMostSignificantBits(), y.getMostSignificantBits());
        if (hi != 0) return hi;
        return Long.compareUnsigned(x.getLeastSignificantBits(), y.getLeastSignificantBits());
    }
}
