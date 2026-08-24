package com.tassist.application.messaging;

import com.tassist.application.generation.GenerationService;
import com.tassist.application.generation.GenerationService.GenerationOutcome;
import com.tassist.application.generation.GenerationService.Mode;
import com.tassist.domain.model.Chunk;
import com.tassist.domain.model.ConversationMessage;
import com.tassist.domain.port.in.QuotaUseCase;
import com.tassist.domain.port.in.RetrievalUseCase;
import com.tassist.domain.port.in.RetrievalUseCase.RetrievalResult;
import com.tassist.domain.port.in.RetrievalUseCase.TextHit;
import com.tassist.domain.port.out.ChannelFileRepository;
import com.tassist.domain.port.out.FileRepository;
import com.tassist.domain.model.ChannelFile;
import com.tassist.domain.vo.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/** Unit tests for AI-in-thread trigger detection + grounded/fallback wiring (§7). */
class ConversationAiServiceTest {

    ConversationService conversations;
    RetrievalUseCase retrieval;
    GenerationService generation;
    ChannelFileRepository channelFiles;
    FileRepository files;
    QuotaUseCase quota;
    ConversationAiService svc;

    ChannelId channelId; ConversationId convId; UserId invoker;

    @BeforeEach void setup() {
        conversations = mock(ConversationService.class);
        retrieval = mock(RetrievalUseCase.class);
        generation = mock(GenerationService.class);
        channelFiles = mock(ChannelFileRepository.class);
        files = mock(FileRepository.class);
        quota = mock(QuotaUseCase.class);
        svc = new ConversationAiService(conversations, retrieval, generation, channelFiles, files, quota);
        channelId = ChannelId.newId(); convId = ConversationId.newId(); invoker = UserId.newId();
        // saveAiMessage echoes an AI message so we can assert on content
        when(conversations.saveAiMessage(any(), any(), any())).thenAnswer(inv ->
            ConversationMessage.ai(ConversationMessageId.newId(), inv.getArgument(0),
                inv.getArgument(1), inv.getArgument(2), Instant.now()));
    }

    // ── trigger detection ──
    @Test void detects_ai_and_assist_caseInsensitive() {
        assertThat(ConversationAiService.mentionsAi("hey @ai what is X")).isTrue();
        assertThat(ConversationAiService.mentionsAi("please @Assist me")).isTrue();
        assertThat(ConversationAiService.mentionsAi("@AI")).isTrue();
    }
    @Test void ignores_plainText_and_emails_and_words() {
        assertThat(ConversationAiService.mentionsAi("just a normal message")).isFalse();
        assertThat(ConversationAiService.mentionsAi("email me at bob@aixyz.com")).isFalse();
        assertThat(ConversationAiService.mentionsAi("the airplane is @airport")).isFalse(); // @airport != @ai token
        assertThat(ConversationAiService.mentionsAi("assist without at-sign")).isFalse();
    }
    @Test void stripsTrigger_leavesQuestion() {
        assertThat(ConversationAiService.stripTrigger("@ai what is recursion?")).isEqualTo("what is recursion?");
        assertThat(ConversationAiService.stripTrigger("hey @assist explain sprint 3")).isEqualTo("hey explain sprint 3");
    }

    // ── no trigger → no AI turn ──
    @Test void noTrigger_returnsEmpty_noGeneration() {
        Optional<ConversationMessage> out = svc.maybeRespond(invoker, channelId, convId, "just chatting");
        assertThat(out).isEmpty();
        verifyNoInteractions(retrieval, generation);
    }

    // ── tagged but empty question → gentle nudge, still safe ──
    @Test void taggedButEmpty_savesNudge_noGeneration() {
        Optional<ConversationMessage> out = svc.maybeRespond(invoker, channelId, convId, "@ai");
        assertThat(out).isPresent();
        assertThat(out.get().senderKind()).isEqualTo(MessageSenderKind.AI);
        verifyNoInteractions(retrieval, generation);
    }

    // ── grounded answer with citation labelled by display_label (§7.5) ──
    @Test void groundedAnswer_usesChannelDisplayLabel_notFilename() {
        FileId fileId = FileId.newId();
        Chunk chunk = new Chunk(ChunkId.newId(), fileId, 0, "Sprint 3 added feasibility.",
            Map.of("page", "6"), null);
        RetrievalResult retrieved = new RetrievalResult(List.of(new TextHit(chunk, 0.9)), List.of(), false, List.of());
        when(retrieval.retrieve(any())).thenReturn(retrieved);
        when(generation.generate(any(), any(), eq(false)))
            .thenReturn(new GenerationOutcome("Sprint 3 added feasibility [S1].", Mode.GROUNDED, 10, 20, List.of()));
        when(channelFiles.findByChannel(channelId))
            .thenReturn(List.of(new ChannelFile(channelId, fileId, "Lecture 6 — Sprints", Instant.now())));

        Optional<ConversationMessage> out = svc.maybeRespond(invoker, channelId, convId, "@ai what did sprint 3 add?");
        assertThat(out).isPresent();
        assertThat(out.get().citations()).hasSize(1);
        assertThat(out.get().citations().get(0).displayLabel()).isEqualTo("Lecture 6 — Sprints");
        verify(quota).recordQuestion(eq(invoker), anyLong());
    }

    // ── no hits → fallback answer, no citations, no crash ──
    @Test void noHits_fallback_noCitations() {
        when(retrieval.retrieve(any())).thenReturn(new RetrievalResult(List.of(), List.of(), true, List.of()));
        when(generation.generate(any(), any(), eq(false)))
            .thenReturn(new GenerationOutcome("I couldn't find that in the channel's materials.", Mode.FALLBACK, 5, 8, List.of()));
        Optional<ConversationMessage> out = svc.maybeRespond(invoker, channelId, convId, "@ai unrelated question");
        assertThat(out).isPresent();
        assertThat(out.get().citations()).isEmpty();
    }

    // ── generation throws → graceful inline message, never propagates ──
    @Test void generationFailure_savesGracefulMessage_doesNotThrow() {
        when(retrieval.retrieve(any())).thenThrow(new RuntimeException("upstream down"));
        Optional<ConversationMessage> out = svc.maybeRespond(invoker, channelId, convId, "@ai hello");
        assertThat(out).isPresent();
        assertThat(out.get().senderKind()).isEqualTo(MessageSenderKind.AI);
        assertThat(out.get().content()).contains("couldn't answer");
    }
}
