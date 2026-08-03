package com.tassist.domain.port.out;

import com.tassist.domain.model.Chat;
import com.tassist.domain.vo.ChatId;
import com.tassist.domain.vo.UserId;
import java.util.List;
import java.util.Optional;

/** Persistence port for {@link Chat} (spec §7). */
public interface ChatRepository {
    Chat save(Chat chat);
    Optional<Chat> findById(ChatId id);
    List<Chat> findByOwner(UserId ownerId);
    void delete(ChatId id);
}
