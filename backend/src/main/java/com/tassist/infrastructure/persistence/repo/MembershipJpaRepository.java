package com.tassist.infrastructure.persistence.repo;

import com.tassist.infrastructure.persistence.entity.MembershipEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MembershipJpaRepository extends JpaRepository<MembershipEntity, UUID> {
    Optional<MembershipEntity> findByChannelIdAndUserId(UUID channelId, UUID userId);
    List<MembershipEntity> findByChannelIdAndStatus(UUID channelId, MembershipEntity.StatusDb status);
    List<MembershipEntity> findByChannelId(UUID channelId);
}
