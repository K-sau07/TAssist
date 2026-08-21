package com.tassist.infrastructure.persistence.repo;

import com.tassist.infrastructure.persistence.entity.ChannelEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChannelJpaRepository extends JpaRepository<ChannelEntity, UUID> {
    Optional<ChannelEntity> findByUsername(String username);
    List<ChannelEntity> findByOwnerId(UUID ownerId);
    boolean existsByUsername(String username);

    @Query("select c from ChannelEntity c where c.visibility = 'PUBLIC' " +
           "and (lower(c.username) like %:q% or lower(c.displayName) like %:q%) order by c.username")
    java.util.List<ChannelEntity> search(@Param("q") String q, Pageable pageable);

    @Query("select c from ChannelEntity c where c.visibility = 'PUBLIC' order by c.createdAt desc")
    java.util.List<ChannelEntity> findPublic(Pageable pageable);
}
