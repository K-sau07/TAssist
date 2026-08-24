package com.tassist.infrastructure.persistence.repo;

import com.tassist.infrastructure.persistence.entity.ConversationMessageEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ConversationMessageJpaRepository extends JpaRepository<ConversationMessageEntity, UUID> {

    List<ConversationMessageEntity> findByConversationIdOrderByCreatedAtAsc(UUID conversationId);

    // page backwards from a cursor: messages older than `before`, newest-first (caller re-sorts asc)
    @Query("""
        select m from ConversationMessageEntity m
        where m.conversationId = :conversationId and m.createdAt < :before
        order by m.createdAt desc
        """)
    List<ConversationMessageEntity> findBefore(@Param("conversationId") UUID conversationId,
                                               @Param("before") Instant before, Pageable pageable);

    @Query("""
        select m from ConversationMessageEntity m
        where m.conversationId = :conversationId
        order by m.createdAt desc
        """)
    List<ConversationMessageEntity> findLatestPage(@Param("conversationId") UUID conversationId, Pageable pageable);

    @Query("""
        select m from ConversationMessageEntity m
        where m.conversationId = :conversationId and m.deletedAt is null
        order by m.createdAt desc
        """)
    List<ConversationMessageEntity> findLatestVisible(@Param("conversationId") UUID conversationId, Pageable pageable);

    @Query("""
        select count(m) from ConversationMessageEntity m
        where m.conversationId = :conversationId and m.createdAt > :after
          and m.deletedAt is null and (m.senderId is null or m.senderId <> :excludeSender)
        """)
    long countUnread(@Param("conversationId") UUID conversationId,
                     @Param("after") Instant after, @Param("excludeSender") UUID excludeSender);
}
