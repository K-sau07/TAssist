package com.tassist.infrastructure.persistence.adapter;

import com.tassist.domain.model.Message;
import com.tassist.domain.port.out.MessageRepository;
import com.tassist.domain.vo.ChatId;
import com.tassist.infrastructure.persistence.mapper.MessageMapper;
import com.tassist.infrastructure.persistence.repo.MessageJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Repository
public class MessageRepositoryAdapter implements MessageRepository {
    private final MessageJpaRepository jpa;
    public MessageRepositoryAdapter(MessageJpaRepository jpa) { this.jpa = jpa; }

    @Override public Message save(Message m) { return MessageMapper.toDomain(jpa.save(MessageMapper.toEntity(m))); }
    @Override public List<Message> findByChat(ChatId chatId) { return jpa.findByChatIdOrderByCreatedAtAsc(chatId.value()).stream().map(MessageMapper::toDomain).toList(); }
    @Override @Transactional public void deleteByChat(ChatId chatId) { jpa.deleteByChatId(chatId.value()); }
}
