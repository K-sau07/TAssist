package com.tassist.infrastructure.persistence.adapter;

import com.tassist.domain.model.Membership;
import com.tassist.domain.port.out.MembershipRepository;
import com.tassist.domain.vo.ChannelId;
import com.tassist.domain.vo.MembershipId;
import com.tassist.domain.vo.MembershipStatus;
import com.tassist.domain.vo.UserId;
import com.tassist.infrastructure.persistence.entity.MembershipEntity;
import com.tassist.infrastructure.persistence.mapper.MembershipMapper;
import com.tassist.infrastructure.persistence.repo.MembershipJpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public class MembershipRepositoryAdapter implements MembershipRepository {
    private final MembershipJpaRepository jpa;
    public MembershipRepositoryAdapter(MembershipJpaRepository jpa) { this.jpa = jpa; }

    @Override public Membership save(Membership m) { return MembershipMapper.toDomain(jpa.save(MembershipMapper.toEntity(m))); }
    @Override public Optional<Membership> findById(MembershipId id) { return jpa.findById(id.value()).map(MembershipMapper::toDomain); }
    @Override public Optional<Membership> findByChannelAndUser(ChannelId c, UserId u) { return jpa.findByChannelIdAndUserId(c.value(), u.value()).map(MembershipMapper::toDomain); }
    @Override public List<Membership> findByChannelAndStatus(ChannelId c, MembershipStatus s) { return jpa.findByChannelIdAndStatus(c.value(), MembershipEntity.StatusDb.valueOf(s.name())).stream().map(MembershipMapper::toDomain).toList(); }
    @Override public List<Membership> findByChannel(ChannelId c) { return jpa.findByChannelId(c.value()).stream().map(MembershipMapper::toDomain).toList(); }
}
