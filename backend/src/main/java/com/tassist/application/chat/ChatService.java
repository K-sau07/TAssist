package com.tassist.application.chat;

import com.tassist.domain.error.Forbidden;
import com.tassist.domain.error.NotFoundError;
import com.tassist.domain.error.ValidationError;
import com.tassist.domain.model.Chat;
import com.tassist.domain.model.Message;
import com.tassist.domain.port.in.ChatUseCase;
import com.tassist.domain.port.out.ChatRepository;
import com.tassist.domain.port.out.FolderRepository;
import com.tassist.domain.port.out.MessageRepository;
import com.tassist.domain.vo.ChatId;
import com.tassist.domain.vo.ChatScope;
import com.tassist.domain.vo.FolderId;
import com.tassist.domain.vo.UserId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Private-library chat lifecycle (§12.4). REGULAR + FOLDER scopes only; CHANNEL chats are
 * created through the channel flow (Step 12+), not here (D18). Ownership enforced (§7.4).
 */
@Service
public class ChatService implements ChatUseCase {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private static final String DEFAULT_TITLE = "New chat";

    private final ChatRepository chats;
    private final MessageRepository messages;
    private final FolderRepository folders;

    public ChatService(ChatRepository chats, MessageRepository messages, FolderRepository folders) {
        this.chats = chats;
        this.messages = messages;
        this.folders = folders;
    }

    @Override
    public Chat create(UserId actingUser, CreateChatCommand cmd) {
        if (cmd.scope() == ChatScope.CHANNEL) {
            throw new ValidationError("channel chats are not created via this endpoint");
        }
        Optional<FolderId> folderId = Optional.empty();
        if (cmd.scope() == ChatScope.FOLDER) {
            FolderId fid = cmd.folderId()
                .orElseThrow(() -> new ValidationError("folderId required for FOLDER scope"));
            // ownership: the folder must belong to the acting user
            var folder = folders.findById(fid)
                .orElseThrow(() -> new NotFoundError("folder not found"));
            if (!folder.ownerId().equals(actingUser)) throw new Forbidden("not your folder");
            folderId = Optional.of(fid);
        }
        Instant now = Instant.now();
        Chat chat = new Chat(ChatId.newId(), actingUser, cmd.scope(), folderId,
            Optional.empty(), DEFAULT_TITLE, now, now);
        Chat saved = chats.save(chat);
        log.info("Chat created: {} scope={} owner={}", saved.id().value(), cmd.scope(), actingUser.value());
        return saved;
    }

    @Override
    public List<Chat> list(UserId actingUser) {
        // §12.4: exclude channel chats from the private library listing
        return chats.findByOwner(actingUser).stream()
            .filter(c -> c.scope() != ChatScope.CHANNEL)
            .toList();
    }

    @Override
    public Chat get(UserId actingUser, ChatId chatId) {
        return ownedChat(actingUser, chatId);
    }

    @Override
    public List<Message> getMessages(UserId actingUser, ChatId chatId) {
        ownedChat(actingUser, chatId);
        return messages.findByChat(chatId);
    }

    @Override
    public Chat rename(UserId actingUser, ChatId chatId, String newTitle) {
        Chat chat = ownedChat(actingUser, chatId);
        if (newTitle == null || newTitle.isBlank()) throw new ValidationError("title must not be blank");
        String clean = newTitle.strip();
        if (clean.length() > 200) throw new ValidationError("title too long (max 200)");
        Chat renamed = new Chat(chat.id(), chat.ownerId(), chat.scope(), chat.folderId(),
            chat.channelId(), clean, chat.createdAt(), Instant.now());
        return chats.save(renamed);
    }

    @Override
    public void delete(UserId actingUser, ChatId chatId) {
        ownedChat(actingUser, chatId);
        messages.deleteByChat(chatId);
        chats.delete(chatId);
        log.info("Chat deleted: {}", chatId.value());
    }

    Chat ownedChat(UserId actingUser, ChatId chatId) {
        Chat chat = chats.findById(chatId)
            .orElseThrow(() -> new NotFoundError("chat not found"));
        if (!chat.ownerId().equals(actingUser)) throw new Forbidden("not your chat");
        return chat;
    }
}
