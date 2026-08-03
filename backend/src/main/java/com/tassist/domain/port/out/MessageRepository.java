package com.tassist.domain.port.out;

import com.tassist.domain.model.Message;
import com.tassist.domain.vo.ChatId;
import java.util.List;

/** Persistence port for {@link Message} (owned by Chat) (spec §7). */
public interface MessageRepository {
    Message save(Message message);
    List<Message> findByChat(ChatId chatId);
    void deleteByChat(ChatId chatId);
}
