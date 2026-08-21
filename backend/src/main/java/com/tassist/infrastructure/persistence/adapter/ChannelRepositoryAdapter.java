package com.tassist.infrastructure.persistence.adapter;

import com.tassist.domain.model.Channel;
import com.tassist.domain.port.out.ChannelRepository;
import com.tassist.domain.vo.ChannelId;
import com.tassist.domain.vo.UserId;
import com.tassist.infrastructure.persistence.mapper.ChannelMapper;
import com.tassist.infrastructure.persistence.repo.ChannelJpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public class ChannelRepositoryAdapter implements ChannelRepository {
    private final ChannelJpaRepository jpa;
    public ChannelRepositoryAdapter(ChannelJpaRepository jpa) { this.jpa = jpa; }

    @Override public Channel save(Channel c) { return ChannelMapper.toDomain(jpa.save(ChannelMapper.toEntity(c))); }
    @Override public Optional<Channel> findById(ChannelId id) { return jpa.findById(id.value()).map(ChannelMapper::toDomain); }
    @Override public Optional<Channel> findByUsername(String u) { return jpa.findByUsername(u).map(ChannelMapper::toDomain); }
    @Override public List<Channel> findByOwner(UserId ownerId) { return jpa.findByOwnerId(ownerId.value()).stream().map(ChannelMapper::toDomain).toList(); }
    @Override public boolean existsByUsername(String u) { return jpa.existsByUsername(u); }

    @Override
    public List<Channel> searchByUsernameOrDisplayName(String queryLowercased, int limit) {
        return jpa.search(queryLowercased, org.springframework.data.domain.PageRequest.of(0, Math.max(1, limit)))
            .stream().map(ChannelMapper::toDomain).toList();
    }

    @Override
    public List<Channel> findPublic(int page, int pageSize) {
        return jpa.findPublic(org.springframework.data.domain.PageRequest.of(Math.max(0, page), Math.max(1, pageSize)))
            .stream().map(ChannelMapper::toDomain).toList();
    }
    @Override public void delete(ChannelId id) { jpa.deleteById(id.value()); }
}
