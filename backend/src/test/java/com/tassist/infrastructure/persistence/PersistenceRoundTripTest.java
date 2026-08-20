package com.tassist.infrastructure.persistence;

import com.tassist.domain.model.*;
import com.tassist.domain.port.out.*;
import com.tassist.domain.vo.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class PersistenceRoundTripTest extends AbstractPgvectorContainerTest {

    @Autowired UserRepository users;
    @Autowired FileRepository files;
    @Autowired ChunkRepository chunks;
    @Autowired ChatRepository chats;
    @Autowired MessageRepository messages;
    @Autowired QuotaUsageRepository quotas;

    private User newUser(String email) {
        Instant now = Instant.now();
        return new User(UserId.newId(), email, "Test User",
            Optional.of("$2a$hash"), AuthProvider.PASSWORD, Optional.empty(), now, now);
    }

    private float[] vec(float seed) {
        float[] v = new float[1024];
        for (int i = 0; i < 1024; i++) v[i] = seed + i * 0.0001f;
        return v;
    }

    @Test
    void user_roundtrips_with_enum_and_citext() {
        // Domain enforces lowercased email; persistence uses CITEXT for case-insensitive lookup.
        User saved = users.save(newUser("alice@example.com"));
        Optional<User> byId = users.findById(saved.id());
        assertThat(byId).isPresent();
        assertThat(byId.get().authProvider()).isEqualTo(AuthProvider.PASSWORD);
        // Port contract: callers pass a lowercased email (param named emailLowercased).
        assertThat(users.findByEmail("alice@example.com")).isPresent();
        assertThat(users.existsByEmail("alice@example.com")).isTrue();
    }

    @Test
    void chunk_vector_saves_and_similarity_search_ranks_nearest() {
        User u = users.save(newUser("vec@example.com"));
        Instant now = Instant.now();
        File f = files.save(new File(FileId.newId(), u.id(), "doc.pdf", FileType.PDF,
            100L, "key/doc.pdf", "hash-" + System.nanoTime(), FileStatus.READY, Optional.empty(), now, now));

        Chunk near = new Chunk(ChunkId.newId(), f.id(), 0, "near text", Map.of("page", "1"), vec(0.10f));
        Chunk far  = new Chunk(ChunkId.newId(), f.id(), 1, "far text",  Map.of("page", "2"), vec(0.90f));
        chunks.saveAll(List.of(near, far));

        assertThat(chunks.countByFile(f.id())).isEqualTo(2);

        // Query vector closest to `near`
        List<ChunkRepository.ScoredChunk> hits =
            chunks.searchSimilar(vec(0.10f), List.of(f.id()), 2);
        assertThat(hits).hasSize(2);
        assertThat(hits.get(0).chunk().text()).isEqualTo("near text");
        assertThat(hits.get(0).similarity()).isGreaterThan(hits.get(1).similarity());
    }

    @Test
    void message_jsonb_citations_and_mentions_roundtrip() {
        User u = users.save(newUser("msg@example.com"));
        Instant now = Instant.now();
        Chat chat = chats.save(new Chat(ChatId.newId(), u.id(), ChatScope.REGULAR,
            Optional.empty(), Optional.empty(), "My chat", now, now));

        FileId fid = FileId.newId();
        ChunkId cid = ChunkId.newId();
        Message assistant = new Message(MessageId.newId(), chat.id(), MessageRole.ASSISTANT,
            "Here is the answer",
            List.of(new Citation(fid, cid, "doc.pdf p.1", Optional.of("snippet text"))),
            List.of(), now);
        Message user = new Message(MessageId.newId(), chat.id(), MessageRole.USER,
            "the question", List.of(), List.of(fid), now.plusMillis(1));

        messages.save(assistant);
        messages.save(user);

        List<Message> back = messages.findByChat(chat.id());
        assertThat(back).hasSize(2);
        Message a = back.get(0);
        assertThat(a.role()).isEqualTo(MessageRole.ASSISTANT);
        assertThat(a.citations()).hasSize(1);
        assertThat(a.citations().get(0).fileId()).isEqualTo(fid);
        assertThat(a.citations().get(0).chunkId()).isEqualTo(cid);
        assertThat(a.citations().get(0).snippet()).contains("snippet text");
        Message q = back.get(1);
        assertThat(q.mentionedFiles()).containsExactly(fid);
    }

    @Test
    void quota_usage_composite_key_and_yearmonth_roundtrip() {
        User u = users.save(newUser("quota@example.com"));
        YearMonth period = YearMonth.of(2026, 8);
        quotas.save(new QuotaUsage(u.id(), period, 5, 2, 1024, 999));
        Optional<QuotaUsage> back = quotas.find(u.id(), period);
        assertThat(back).isPresent();
        assertThat(back.get().questionsAsked()).isEqualTo(5);
        assertThat(back.get().period()).isEqualTo(period);
    }
}
