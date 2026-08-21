package com.tassist.domain.port.in;

import com.tassist.domain.model.Chat;
import com.tassist.domain.model.Message;
import com.tassist.domain.vo.ChatId;
import com.tassist.domain.vo.ChatScope;
import com.tassist.domain.vo.FolderId;
import com.tassist.domain.vo.UserId;
import java.util.List;
import java.util.Optional;

/**
 * Inbound port: private-library chat lifecycle (spec 12.4). Ownership verified in impl (7.4).
 * SSE streaming of a sent message is a separate service (Step 11); this port covers the CRUD
 * lifecycle plus the persisted-message reads the streaming layer builds on.
 */
public interface ChatUseCase {

    Chat create(UserId actingUser, CreateChatCommand command);

    List<Chat> list(UserId actingUser);

    Chat get(UserId actingUser, ChatId chatId);

    List<Message> getMessages(UserId actingUser, ChatId chatId);

    Chat rename(UserId actingUser, ChatId chatId, String newTitle);

    void delete(UserId actingUser, ChatId chatId);

    /** Send a user message; runs retrieval+generation and persists both messages (non-streaming, Step 10). */
    SendResult sendMessage(UserId actingUser, ChatId chatId, String content);

    record SendResult(Message userMessage, Message assistantMessage, String mode, List<String> warnings) {}

    record CreateChatCommand(ChatScope scope, Optional<FolderId> folderId) {
        public CreateChatCommand {
            if (scope == null) throw new IllegalArgumentException("CreateChatCommand.scope must not be null");
            folderId = folderId == null ? Optional.empty() : folderId;
        }
    }
}
