package com.tassist.application.chat;

import com.tassist.domain.error.Forbidden;
import com.tassist.domain.error.NotFoundError;
import com.tassist.domain.error.ValidationError;
import com.tassist.domain.model.*;
import com.tassist.domain.port.in.ChatUseCase.CreateChatCommand;
import com.tassist.domain.port.out.*;
import com.tassist.domain.vo.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

/** Unit tests for §12.4 chat lifecycle (fakes, no Spring/DB). */
class ChatServiceTest {

    static class FakeChats implements ChatRepository {
        final Map<UUID, Chat> byId = new LinkedHashMap<>();
        public Chat save(Chat c) { byId.put(c.id().value(), c); return c; }
        public Optional<Chat> findById(ChatId id) { return Optional.ofNullable(byId.get(id.value())); }
        public List<Chat> findByOwner(UserId o) {
            return byId.values().stream().filter(c -> c.ownerId().equals(o)).toList(); }
        public void delete(ChatId id) { byId.remove(id.value()); }
    }
    static class FakeMessages implements MessageRepository {
        final List<Message> all = new ArrayList<>();
        public Message save(Message m) { all.add(m); return m; }
        public List<Message> findByChat(ChatId id) {
            return all.stream().filter(m -> m.chatId().equals(id)).toList(); }
        public void deleteByChat(ChatId id) { all.removeIf(m -> m.chatId().equals(id)); }
    }
    static class FakeFolders implements FolderRepository {
        final Map<UUID, Folder> byId = new HashMap<>();
        public Folder save(Folder f) { byId.put(f.id().value(), f); return f; }
        public Optional<Folder> findById(FolderId id) { return Optional.ofNullable(byId.get(id.value())); }
        public List<Folder> findByOwner(UserId o) { return List.of(); }
        public boolean existsByOwnerAndName(UserId o, String n) { return false; }
        public void delete(FolderId id) {}
    }

    private final FakeChats chats = new FakeChats();
    private final FakeMessages messages = new FakeMessages();
    private final FakeFolders folders = new FakeFolders();
    private final ChatService svc = new ChatService(chats, messages, folders);
    private final UserId user = UserId.newId();
    private final UserId other = UserId.newId();

    @Test void create_regular_chat() {
        Chat c = svc.create(user, new CreateChatCommand(ChatScope.REGULAR, Optional.empty()));
        assertThat(c.scope()).isEqualTo(ChatScope.REGULAR);
        assertThat(c.title()).isEqualTo("New chat");
    }

    @Test void create_folder_chat_requires_owned_folder() {
        Folder f = folders.save(new Folder(FolderId.newId(), user, "F", Instant.now()));
        Chat c = svc.create(user, new CreateChatCommand(ChatScope.FOLDER, Optional.of(f.id())));
        assertThat(c.scope()).isEqualTo(ChatScope.FOLDER);
        assertThat(c.folderId()).contains(f.id());
    }

    @Test void folder_chat_without_folderId_rejected() {
        assertThatThrownBy(() -> svc.create(user, new CreateChatCommand(ChatScope.FOLDER, Optional.empty())))
            .isInstanceOf(ValidationError.class);
    }

    @Test void folder_chat_on_others_folder_forbidden() {
        Folder f = folders.save(new Folder(FolderId.newId(), other, "F", Instant.now()));
        assertThatThrownBy(() -> svc.create(user, new CreateChatCommand(ChatScope.FOLDER, Optional.of(f.id()))))
            .isInstanceOf(Forbidden.class);
    }

    @Test void channel_scope_rejected_here() {
        assertThatThrownBy(() -> svc.create(user, new CreateChatCommand(ChatScope.CHANNEL, Optional.empty())))
            .isInstanceOf(ValidationError.class);
    }

    @Test void list_excludes_channel_chats() {
        svc.create(user, new CreateChatCommand(ChatScope.REGULAR, Optional.empty()));
        // inject a channel chat directly (bypassing create, which forbids it)
        Instant now = Instant.now();
        chats.save(new Chat(ChatId.newId(), user, ChatScope.CHANNEL, Optional.empty(),
            Optional.of(ChannelId.newId()), "ch", now, now));
        assertThat(svc.list(user)).hasSize(1);
        assertThat(svc.list(user).get(0).scope()).isEqualTo(ChatScope.REGULAR);
    }

    @Test void rename_chat() {
        Chat c = svc.create(user, new CreateChatCommand(ChatScope.REGULAR, Optional.empty()));
        Chat r = svc.rename(user, c.id(), "My topic");
        assertThat(r.title()).isEqualTo("My topic");
    }

    @Test void get_others_chat_forbidden() {
        Chat c = svc.create(user, new CreateChatCommand(ChatScope.REGULAR, Optional.empty()));
        assertThatThrownBy(() -> svc.get(other, c.id())).isInstanceOf(Forbidden.class);
    }

    @Test void get_missing_chat_not_found() {
        assertThatThrownBy(() -> svc.get(user, ChatId.newId())).isInstanceOf(NotFoundError.class);
    }

    @Test void delete_removes_chat_and_messages() {
        Chat c = svc.create(user, new CreateChatCommand(ChatScope.REGULAR, Optional.empty()));
        messages.save(new Message(MessageId.newId(), c.id(), MessageRole.USER, "hi",
            List.of(), List.of(), Instant.now()));
        svc.delete(user, c.id());
        assertThat(chats.findById(c.id())).isEmpty();
        assertThat(messages.findByChat(c.id())).isEmpty();
    }
}
