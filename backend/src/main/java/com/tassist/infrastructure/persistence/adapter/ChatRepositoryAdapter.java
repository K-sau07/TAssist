package com.tassist.infrastructure.persistence.adapter;

import com.tassist.domain.model.Chat;
import com.tassist.domain.port.out.ChatRepository;
import com.tassist.domain.vo.ChatId;
import com.tassist.domain.vo.UserId;
import com.tassist.infrastructure.persistence.mapper.ChatMapper;
import com.tassist.infrastructure.persistence.repo.ChatJpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public class ChatRepositoryAdapter implements ChatRepository {
    private final ChatJpaRepository jpa;
    public ChatRepositoryAdapter(ChatJpaRepository jpa) { this.jpa = jpa; }

    @Override public Chat save(Chat chat) { return ChatMapper.toDomain(jpa.save(ChatMapper.toEntity(chat))); }
    @Override public Optional<Chat> findById(ChatId id) { return jpa.findById(id.value()).map(ChatMapper::toDomain); }
    @Override public List<Chat> findByOwner(UserId ownerId) { return jpa.findByOwnerIdOrderByUpdatedAtDesc(ownerId.value()).stream().map(ChatMapper::toDomain).toList(); }
    @Override public void delete(ChatId id) { jpa.deleteById(id.value()); }
}
