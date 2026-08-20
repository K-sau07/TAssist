package com.tassist.infrastructure.persistence.repo;

import com.tassist.infrastructure.persistence.entity.ChannelEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChannelJpaRepository extends JpaRepository<ChannelEntity, UUID> {
    Optional<ChannelEntity> findByUsername(String username);
    List<ChannelEntity> findByOwnerId(UUID ownerId);
    boolean existsByUsername(String username);
}
