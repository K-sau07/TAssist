package com.tassist.infrastructure.persistence.repo;

import com.tassist.infrastructure.persistence.entity.MessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

public interface MessageJpaRepository extends JpaRepository<MessageEntity, UUID> {
    List<MessageEntity> findByChatIdOrderByCreatedAtAsc(UUID chatId);
    @Modifying @Query("delete from MessageEntity m where m.chatId = :chatId")
    void deleteByChatId(@Param("chatId") UUID chatId);
}
