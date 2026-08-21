package com.tassist.application.chat;

import com.tassist.application.generation.GenerationService;
import com.tassist.application.generation.PromptBuilder;
import com.tassist.application.retrieval.MentionResolver;
import com.tassist.domain.model.*;
import com.tassist.domain.port.in.ChatUseCase.CreateChatCommand;
import com.tassist.domain.port.in.ChatUseCase.SendResult;
import com.tassist.domain.port.in.RetrievalUseCase;
import com.tassist.domain.port.in.RetrievalUseCase.*;
import com.tassist.domain.port.out.*;
import com.tassist.domain.vo.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.YearMonth;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

/** Unit tests for the non-streaming send-message orchestration (§11.6 non-stream, Step 10). */
class ChatSendMessageTest {

    // reuse simple fakes
    static class FakeChats implements ChatRepository {
        final Map<UUID, Chat> byId = new LinkedHashMap<>();
        public Chat save(Chat c) { byId.put(c.id().value(), c); return c; }
        public Optional<Chat> findById(ChatId id) { return Optional.ofNullable(byId.get(id.value())); }
        public List<Chat> findByOwner(UserId o) { return new ArrayList<>(byId.values()); }
        public void delete(ChatId id) { byId.remove(id.value()); }
    }
    static class FakeMessages implements MessageRepository {
        final List<Message> all = new ArrayList<>();
        public Message save(Message m) { all.add(m); return m; }
        public List<Message> findByChat(ChatId id) {
            return all.stream().filter(m -> m.chatId().equals(id)).toList(); }
        public void deleteByChat(ChatId id) {}
    }
    static class FakeFolders implements FolderRepository {
        public Folder save(Folder f) { return f; }
        public Optional<Folder> findById(FolderId id) { return Optional.empty(); }
        public List<Folder> findByOwner(UserId o) { return List.of(); }
        public boolean existsByOwnerAndName(UserId o, String n) { return false; }
        public void delete(FolderId id) {}
    }
    static class FakeFiles implements FileRepository {
        final Map<UUID, File> byId = new HashMap<>();
        public File save(File f) { byId.put(f.id().value(), f); return f; }
        public Optional<File> findById(FileId id) { return Optional.ofNullable(byId.get(id.value())); }
        public List<File> findByOwner(UserId o) { return new ArrayList<>(byId.values()); }
        public Optional<File> findByOwnerAndContentHash(UserId o, String h) { return Optional.empty(); }
        public List<File> findByOwnerAndFilename(UserId o, String n) {
            return byId.values().stream().filter(f -> f.ownerId().equals(o) && f.originalFilename().equals(n)).toList(); }
        public void delete(FileId id) {}
    }
    static class FakeQuota implements QuotaUsageRepository {
        QuotaUsage saved;
        public QuotaUsage save(QuotaUsage u) { saved = u; return u; }
        public Optional<QuotaUsage> find(UserId u, YearMonth p) { return Optional.ofNullable(saved); }
    }
    static class FakeLlm implements LLMClient {
        String reply;
        public LlmResponse complete(LlmRequest r) { return new LlmResponse(reply, 100, 20); }
        public void stream(LlmRequest r, StreamEvents e) {}
    }
    // retrieval stub returning preset hits
    static class StubRetrieval implements RetrievalUseCase {
        RetrievalResult result = new RetrievalResult(List.of(), List.of(), false, List.of());
        Scope lastScope;
        public RetrievalResult retrieve(RetrievalQuery q) { lastScope = q.scope(); return result; }
    }

    private final FakeChats chats = new FakeChats();
    private final FakeMessages messages = new FakeMessages();
    private final FakeFiles files = new FakeFiles();
    private final FakeQuota quota = new FakeQuota();
    private final FakeLlm llm = new FakeLlm();
    private final StubRetrieval retrieval = new StubRetrieval();
    private final MentionResolver resolver = new MentionResolver(files);
    private final GenerationService generation = new GenerationService(new PromptBuilder(), llm, files);
    private final ChatService svc = new ChatService(chats, messages, new FakeFolders(),
        resolver, retrieval, generation, files, quota);

    private final UserId user = UserId.newId();

    private Chat regularChat() {
        return svc.create(user, new CreateChatCommand(ChatScope.REGULAR, Optional.empty()));
    }
    private FileId seedFile(String name) {
        FileId id = FileId.newId();
        files.save(new File(id, user, name, FileType.PDF, 1, "k", "h" + id.value(),
            FileStatus.READY, Optional.empty(), Instant.now(), Instant.now()));
        return id;
    }
    private TextHit hit(FileId f, String text) {
        return new TextHit(new Chunk(ChunkId.newId(), f, 0, text, Map.of("page", "1"), new float[]{1f}), 0.9);
    }

    @Test void regular_message_persists_both_and_bumps_quota() {
        Chat c = regularChat();
        llm.reply = "Hello there!";
        SendResult r = svc.sendMessage(user, c.id(), "hi");
        assertThat(r.userMessage().role()).isEqualTo(MessageRole.USER);
        assertThat(r.assistantMessage().role()).isEqualTo(MessageRole.ASSISTANT);
        assertThat(r.assistantMessage().content()).isEqualTo("Hello there!");
        assertThat(r.mode()).isEqualTo("REGULAR");
        assertThat(retrieval.lastScope).isEqualTo(Scope.REGULAR);
        assertThat(messages.all).hasSize(2);
        assertThat(quota.saved.questionsAsked()).isEqualTo(1);
        assertThat(quota.saved.tokensConsumed()).isEqualTo(120);
    }

    @Test void mention_routes_to_mentions_scope_and_builds_citations() {
        Chat c = regularChat();
        FileId f = seedFile("notes.pdf");
        retrieval.result = new RetrievalResult(List.of(hit(f, "the answer text")), List.of(), false, List.of());
        llm.reply = "Based on the notes, the answer is 42 [S1].";
        SendResult r = svc.sendMessage(user, c.id(), "what does @notes.pdf say?");
        assertThat(retrieval.lastScope).isEqualTo(Scope.MENTIONS);
        assertThat(r.userMessage().mentionedFiles()).containsExactly(f);
        assertThat(r.assistantMessage().citations()).hasSize(1);
        assertThat(r.assistantMessage().citations().get(0).fileId()).isEqualTo(f);
        assertThat(r.assistantMessage().citations().get(0).displayLabel()).isEqualTo("notes.pdf, page 1");
    }

    @Test void unknown_mention_produces_warning() {
        Chat c = regularChat();
        llm.reply = "answer";
        SendResult r = svc.sendMessage(user, c.id(), "explain @ghost.pdf");
        assertThat(r.warnings()).anyMatch(w -> w.contains("ghost.pdf"));
    }

    @Test void citation_markers_out_of_range_are_ignored() {
        Chat c = regularChat();
        FileId f = seedFile("a.pdf");
        retrieval.result = new RetrievalResult(List.of(hit(f, "text")), List.of(), false, List.of());
        llm.reply = "claim [S1] and bogus [S5].";
        SendResult r = svc.sendMessage(user, c.id(), "@a.pdf question");
        assertThat(r.assistantMessage().citations()).hasSize(1); // only S1 valid
    }

    @Test void blank_content_rejected() {
        Chat c = regularChat();
        assertThatThrownBy(() -> svc.sendMessage(user, c.id(), "  "))
            .isInstanceOf(com.tassist.domain.error.ValidationError.class);
    }
}
