package com.tassist.infrastructure.persistence.repo;

import com.tassist.infrastructure.persistence.entity.ConversationEntity;
import com.tassist.infrastructure.persistence.entity.ConversationEntity.KindDb;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationJpaRepository extends JpaRepository<ConversationEntity, UUID> {

    // Enum compared against a bound :kind parameter (not a JPQL literal) so Hibernate binds it
    // through the entity's @JdbcTypeCode(NAMED_ENUM) mapping to the pg 'conversation_kind' type.

    // participants are stored canonically (a<b unsigned); caller pre-sorts them
    @Query("""
        select c from ConversationEntity c
        where c.kind = :kind and c.channelId = :channelId
          and c.participantA = :a and c.participantB = :b
        """)
    Optional<ConversationEntity> findDm(@Param("kind") KindDb kind, @Param("channelId") UUID channelId,
                                        @Param("a") UUID a, @Param("b") UUID b);

    @Query("""
        select c from ConversationEntity c
        where c.kind = :kind and c.channelId = :channelId
        """)
    Optional<ConversationEntity> findGroup(@Param("kind") KindDb kind, @Param("channelId") UUID channelId);

    @Query("""
        select c from ConversationEntity c
        where c.kind = :kind and c.channelId = :channelId
          and (c.participantA = :user or c.participantB = :user)
        order by c.updatedAt desc
        """)
    List<ConversationEntity> findDmsForUser(@Param("kind") KindDb kind, @Param("channelId") UUID channelId,
                                            @Param("user") UUID user);
}
