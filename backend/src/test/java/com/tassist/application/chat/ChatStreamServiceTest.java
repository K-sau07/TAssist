package com.tassist.application.chat;

import com.tassist.domain.model.*;
import com.tassist.domain.port.in.RetrievalUseCase;
import com.tassist.domain.port.in.RetrievalUseCase.*;
import com.tassist.domain.port.out.*;
import com.tassist.domain.port.out.LLMClient.LlmRequest;
import com.tassist.domain.port.out.LLMClient.LlmResponse;
import com.tassist.domain.port.out.LLMClient.StreamEvents;
import com.tassist.application.generation.GenerationService;
import com.tassist.application.generation.PromptBuilder;
import com.tassist.application.retrieval.MentionResolver;
import com.tassist.domain.vo.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.YearMonth;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/** Unit tests for §11.6 streaming orchestration (fakes; no Spring/network). */
class ChatStreamServiceTest {

    // recording sink
    static class RecSink implements StreamSink {
        final List<String> events = new ArrayList<>();
        final List<Map<String,Object>> payloads = new ArrayList<>();
        boolean completed;
        public void emit(String e, Map<String,Object> d) { events.add(e); payloads.add(d); }
        public void complete() { completed = true; }
        public void fail(Throwable t) {}
        List<String> tokensText() {
            List<String> out = new ArrayList<>();
            for (int i=0;i<events.size();i++) if (events.get(i).equals("token")) out.add((String)payloads.get(i).get("text"));
            return out;
        }
    }
    // fake LLM: streams the scripted words as tokens
    static class FakeLlm implements LLMClient {
        String[] tokens; int in=10,out=5;
        FakeLlm(String... t){ this.tokens=t; }
        public LlmResponse complete(LlmRequest r){ return new LlmResponse(String.join("",tokens),in,out); }
        public void stream(LlmRequest r, StreamEvents e){
            for (String t: tokens) e.onToken(t);
            e.onCompleted(in,out);
        }
    }
    static class FakeChats implements ChatRepository {
        final Map<UUID,Chat> m=new HashMap<>();
        public Chat save(Chat c){ m.put(c.id().value(),c); return c; }
        public Optional<Chat> findById(ChatId id){ return Optional.ofNullable(m.get(id.value())); }
        public List<Chat> findByOwner(UserId o){ return List.of(); }
        public void delete(ChatId id){}
    }
    static class FakeMessages implements MessageRepository {
        final List<Message> saved=new ArrayList<>();
        public Message save(Message x){ saved.add(x); return x; }
        public List<Message> findByChat(ChatId id){ return saved; }
        public void deleteByChat(ChatId id){}
    }
    static class FakeFiles implements FileRepository {
        final Map<UUID,File> m=new HashMap<>();
        public File save(File f){ m.put(f.id().value(),f); return f; }
        public Optional<File> findById(FileId id){ return Optional.ofNullable(m.get(id.value())); }
        public List<File> findByOwner(UserId o){ return List.of(); }
        public Optional<File> findByOwnerAndContentHash(UserId o,String h){ return Optional.empty(); }
        public List<File> findByOwnerAndFilename(UserId o,String n){ return List.of(); }
        public void delete(FileId id){}
    }
    static class FakeQuota implements QuotaUsageRepository {
        QuotaUsage last;
        public QuotaUsage save(QuotaUsage q){ last=q; return q; }
        public Optional<QuotaUsage> find(UserId u, YearMonth p){ return Optional.ofNullable(last); }
    }
    static class FakeMentions extends MentionResolver {
        FakeMentions(){ super(new FakeFiles()); }
        public Result resolve(UserId u,String q){ return new Result(List.of(), List.of()); }
    }
    static class FakeRetrieval implements RetrievalUseCase {
        RetrievalResult toReturn = new RetrievalResult(List.of(), List.of(), false, List.of());
        public RetrievalResult retrieve(RetrievalQuery q){ return toReturn; }
    }

    private final FakeChats chats = new FakeChats();
    private final FakeMessages messages = new FakeMessages();
    private final FakeFiles files = new FakeFiles();
    private final FakeQuota quota = new FakeQuota();
    private final FakeRetrieval retrieval = new FakeRetrieval();

    static class FakeChannelFiles implements com.tassist.domain.port.out.ChannelFileRepository {
        public com.tassist.domain.model.ChannelFile add(com.tassist.domain.model.ChannelFile cf){ return cf; }
        public void remove(com.tassist.domain.vo.ChannelId c, FileId f){}
        public java.util.List<com.tassist.domain.model.ChannelFile> findByChannel(com.tassist.domain.vo.ChannelId c){ return java.util.List.of(); }
        public java.util.List<FileId> findFileIdsByChannel(com.tassist.domain.vo.ChannelId c){ return java.util.List.of(); }
    }
    private ChatStreamService svc(LLMClient llm) {
        GenerationService gen = new GenerationService(new PromptBuilder(), llm, files);
        // spreadsheet path is never exercised in these tests (no spreadsheet hits), so a repo-less
        // SpreadsheetQueryService is safe here.
        return new ChatStreamService(chats, messages, new FakeMentions(), retrieval, gen, llm, files, quota,
            new PromptBuilder(), new com.tassist.application.spreadsheet.SpreadsheetQueryService(null),
            new FakeChannelFiles());
    }

    private final UserId user = UserId.newId();
    private ChatId regularChat() {
        ChatId id = ChatId.newId();
        chats.save(new Chat(id, user, ChatScope.REGULAR, Optional.empty(), Optional.empty(),
            "T", Instant.now(), Instant.now()));
        return id;
    }
    private ChatId folderChat() {
        ChatId id = ChatId.newId();
        chats.save(new Chat(id, user, ChatScope.FOLDER, Optional.of(FolderId.newId()), Optional.empty(),
            "T", Instant.now(), Instant.now()));
        return id;
    }
    private TextHit hit(String text) {
        FileId fid = FileId.newId();
        files.save(new File(fid, user, "doc.pdf", FileType.PDF, 1, "k", "h"+fid.value(),
            FileStatus.READY, Optional.empty(), Instant.now(), Instant.now()));
        return new TextHit(new Chunk(ChunkId.newId(), fid, 0, text, Map.of("page","1"), new float[]{1f}), 0.9);
    }

    @Test void regular_scope_streams_tokens_start_and_done() {
        var sink = new RecSink();
        svc(new FakeLlm("Hi", " there")).streamMessage(user, regularChat(), "hello", sink);
        assertThat(sink.events).startsWith("start");
        assertThat(sink.events).endsWith("done");
        assertThat(sink.events).doesNotContain("sources"); // regular skips sources
        assertThat(sink.tokensText()).containsExactly("Hi", " there");
        assertThat(sink.completed).isTrue();
        // USER + ASSISTANT persisted
        assertThat(messages.saved).extracting(Message::role)
            .contains(MessageRole.USER, MessageRole.ASSISTANT);
    }

    @Test void grounded_emits_sources_and_citation_events() {
        retrieval.toReturn = new RetrievalResult(List.of(hit("APAC revenue was 5M")), List.of(), false, List.of());
        var sink = new RecSink();
        // stream an answer containing a [S1] marker
        svc(new FakeLlm("Revenue ", "was 5M ", "[S1]")).streamMessage(user, folderChat(), "revenue?", sink);
        assertThat(sink.events).contains("sources");
        assertThat(sink.events).contains("citation");
        // citation payload references source 1
        int ci = sink.events.indexOf("citation");
        assertThat(sink.payloads.get(ci).get("num")).isEqualTo(1);
        // assistant message persisted with 1 citation
        Message asst = messages.saved.stream().filter(x->x.role()==MessageRole.ASSISTANT).findFirst().orElseThrow();
        assertThat(asst.citations()).hasSize(1);
    }

    @Test void quota_bumped_after_stream() {
        svc(new FakeLlm("x")).streamMessage(user, regularChat(), "hi", new RecSink());
        assertThat(quota.last).isNotNull();
        assertThat(quota.last.questionsAsked()).isEqualTo(1);
        assertThat(quota.last.tokensConsumed()).isEqualTo(15); // 10 in + 5 out
    }

    @Test void not_owned_chat_emits_error() {
        ChatId id = ChatId.newId();
        chats.save(new Chat(id, UserId.newId(), ChatScope.REGULAR, Optional.empty(), Optional.empty(),
            "T", Instant.now(), Instant.now()));
        var sink = new RecSink();
        svc(new FakeLlm("x")).streamMessage(user, id, "hi", sink);
        assertThat(sink.events).contains("error");
        assertThat(sink.completed).isTrue();
    }

    @Test void grounded_sentinel_triggers_fallback_rerun() {
        retrieval.toReturn = new RetrievalResult(List.of(hit("irrelevant")), List.of(), false, List.of());
        var sink = new RecSink();
        // first stream returns exactly the sentinel -> fallback rerun (second start)
        svc(new FakeLlm(PromptBuilder.INSUFFICIENT_SENTINEL)).streamMessage(user, folderChat(), "q?", sink);
        long starts = sink.events.stream().filter(e->e.equals("start")).count();
        assertThat(starts).isEqualTo(2); // grounded start + fallback start
        assertThat(sink.payloads.stream().anyMatch(p->"fallback".equals(p.get("mode")))).isTrue();
    }
}
