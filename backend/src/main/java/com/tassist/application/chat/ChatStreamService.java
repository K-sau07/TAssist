package com.tassist.application.chat;

import com.tassist.domain.error.Forbidden;
import com.tassist.domain.error.NotFoundError;
import com.tassist.domain.error.ValidationError;
import com.tassist.domain.model.*;
import com.tassist.domain.port.in.RetrievalUseCase;
import com.tassist.domain.port.in.RetrievalUseCase.*;
import com.tassist.domain.port.out.*;
import com.tassist.domain.port.out.LLMClient.LlmRequest;
import com.tassist.domain.port.out.LLMClient.StreamEvents;
import com.tassist.application.generation.CitationLabeler;
import com.tassist.application.generation.GenerationService;
import com.tassist.application.generation.GenerationService.Mode;
import com.tassist.application.generation.PromptBuilder;
import com.tassist.application.retrieval.MentionResolver;
import com.tassist.domain.vo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.YearMonth;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SSE streaming orchestration (§11.6). Mirrors ChatService.sendMessage but streams tokens live and
 * emits start/sources/token/citation/done/error events through a transport-agnostic StreamSink.
 * Tool-use loop (query_spreadsheet) is wired in a later sub-step. Grounded→sentinel rerun handled here.
 */
@Service
public class ChatStreamService {

    private static final Logger log = LoggerFactory.getLogger(ChatStreamService.class);
    private static final Pattern CITE = Pattern.compile("\\[S(\\d+)\\]");

    private final ChatRepository chats;
    private final MessageRepository messages;
    private final MentionResolver mentions;
    private final RetrievalUseCase retrieval;
    private final GenerationService generation;
    private final LLMClient llm;
    private final FileRepository files;
    private final QuotaUsageRepository quotas;

    public ChatStreamService(ChatRepository chats, MessageRepository messages, MentionResolver mentions,
                             RetrievalUseCase retrieval, GenerationService generation, LLMClient llm,
                             FileRepository files, QuotaUsageRepository quotas) {
        this.chats = chats;
        this.messages = messages;
        this.mentions = mentions;
        this.retrieval = retrieval;
        this.generation = generation;
        this.llm = llm;
        this.files = files;
        this.quotas = quotas;
    }

    /** Runs the full §11.6 flow, emitting events to {@code sink}. Blocking (caller runs it async). */
    public void streamMessage(UserId actingUser, ChatId chatId, String content, StreamSink sink) {
        try {
            Chat chat = ownedChat(actingUser, chatId);
            if (content == null || content.isBlank()) throw new ValidationError("content must not be blank");

            // 1. Resolve @mentions, persist USER message.
            MentionResolver.Result mentionResult = mentions.resolve(actingUser, content);
            List<FileId> mentionedFiles = mentionResult.fileIds();
            messages.save(new Message(MessageId.newId(), chatId, MessageRole.USER,
                content, List.of(), mentionedFiles, Instant.now()));

            // 2. Scope selection (mentions override chat scope).
            Scope scope;
            if (!mentionedFiles.isEmpty()) scope = Scope.MENTIONS;
            else if (chat.scope() == ChatScope.FOLDER) scope = Scope.FOLDER;
            else scope = Scope.REGULAR;
            boolean regularScope = scope == Scope.REGULAR;

            // 3. Retrieve.
            RetrievalResult retrieved = retrieval.retrieve(new RetrievalQuery(
                actingUser, content, scope, chat.folderId(), Optional.empty(), mentionedFiles));

            // 4. Plan mode + request (shared with non-stream path).
            GenerationService.Plan plan = generation.plan(content, retrieved, regularScope);
            String messageId = MessageId.newId().value().toString();

            // start event
            sink.emit("start", Map.of("messageId", messageId, "mode", modeLabel(plan.mode())));

            // sources event (skip in regular)
            if (plan.mode() != Mode.REGULAR && !plan.sources().isEmpty()) {
                sink.emit("sources", Map.of("sources", sourcesPayload(plan.sources(), retrieved.textHits())));
            }

            // 5. Stream tokens; accumulate; emit citation events as [Sn] complete.
            Accumulator acc = streamAndAccumulate(plan.request(), sink, retrieved.textHits().size());

            String answer = acc.text.toString();
            Mode finalMode = plan.mode();
            int inTok = acc.inputTokens, outTok = acc.outputTokens;

            // 6. Grounded → sentinel → fallback rerun (§11.6 step 7): new start, keep connection.
            if (finalMode == Mode.GROUNDED && answer.strip().equals(PromptBuilder.INSUFFICIENT_SENTINEL)) {
                log.info("Grounded stream hit sentinel; rerunning fallback on same connection.");
                sink.emit("start", Map.of("messageId", messageId, "mode", "fallback"));
                Accumulator fb = streamAndAccumulate(generation.fallbackRequest(content), sink, 0);
                answer = fb.text.toString();
                finalMode = Mode.FALLBACK;
                inTok += fb.inputTokens; outTok += fb.outputTokens;
            }

            // 7. Persist ASSISTANT message with citations.
            List<Citation> citations = buildCitations(answer, retrieved.textHits());
            messages.save(new Message(MessageId.newId(), chatId, MessageRole.ASSISTANT,
                answer, citations, List.of(), Instant.now()));

            // 8. Quota.
            bumpQuota(actingUser, inTok + outTok);

            sink.emit("done", Map.of("messageId", messageId,
                "totalInputTokens", inTok, "totalOutputTokens", outTok));
            sink.complete();
            log.info("Stream done: chat={} scope={} mode={} citations={}",
                chatId.value(), scope, finalMode, citations.size());

        } catch (ValidationError | NotFoundError | Forbidden e) {
            sink.emit("error", Map.of("code", "BAD_REQUEST", "message", e.getMessage()));
            sink.complete();
        } catch (RuntimeException e) {
            log.error("Stream failed: {}", e.toString());
            sink.emit("error", Map.of("code", "UPSTREAM_ERROR", "message", String.valueOf(e.getMessage())));
            sink.complete();
        }
    }

    private static final class Accumulator {
        final StringBuilder text = new StringBuilder();
        int inputTokens, outputTokens;
    }

    /** Stream one LLM request, forwarding token events + emitting citation events; returns accumulated text+usage. */
    private Accumulator streamAndAccumulate(LlmRequest request, StreamSink sink, int sourceCount) {
        Accumulator acc = new Accumulator();
        Set<Integer> citedEmitted = new HashSet<>();
        llm.stream(request, new StreamEvents() {
            public void onToken(String t) {
                int prevLen = acc.text.length();
                acc.text.append(t);
                sink.emit("token", Map.of("text", t));
                // scan the tail for newly-completed [Sn] markers
                Matcher m = CITE.matcher(acc.text);
                while (m.find()) {
                    int n = Integer.parseInt(m.group(1));
                    if (m.end() > prevLen && citedEmitted.add(n) && (sourceCount == 0 || n <= sourceCount)) {
                        sink.emit("citation", Map.of("num", n, "spanStart", m.start(), "spanEnd", m.end()));
                    }
                }
            }
            public void onToolUse(String id, String name, Map<String, Object> input) {
                // tool-use loop wired in the next sub-step; surface the event for now.
                sink.emit("tool_use", Map.of("toolCallId", id, "name", name, "input", input));
            }
            public void onCompleted(int in, int out) { acc.inputTokens = in; acc.outputTokens = out; }
            public void onError(String code, String msg) {
                sink.emit("error", Map.of("code", code, "message", String.valueOf(msg)));
            }
        });
        return acc;
    }

    private List<Map<String, Object>> sourcesPayload(List<PromptBuilder.Source> sources, List<TextHit> hits) {
        List<Map<String, Object>> out = new ArrayList<>(sources.size());
        for (int i = 0; i < sources.size(); i++) {
            PromptBuilder.Source s = sources.get(i);
            TextHit hit = i < hits.size() ? hits.get(i) : null;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("num", i + 1);
            m.put("fileId", hit != null ? hit.chunk().fileId().value().toString() : null);
            m.put("label", s.label());
            m.put("similarity", hit != null ? hit.similarity() : null);
            m.put("snippet", s.text());
            out.add(m);
        }
        return out;
    }

    private List<Citation> buildCitations(String answer, List<TextHit> hits) {
        if (answer == null || hits.isEmpty()) return List.of();
        Set<Integer> cited = new LinkedHashSet<>();
        Matcher m = CITE.matcher(answer);
        while (m.find()) {
            int n = Integer.parseInt(m.group(1));
            if (n >= 1 && n <= hits.size()) cited.add(n);
        }
        List<Citation> out = new ArrayList<>(cited.size());
        for (int n : cited) {
            Chunk chunk = hits.get(n - 1).chunk();
            File f = files.findById(chunk.fileId()).orElse(null);
            String filename = f != null ? f.originalFilename() : "source";
            String label = CitationLabeler.label(filename,
                f != null ? f.type() : FileType.TXT, chunk.metadata());
            out.add(new Citation(chunk.fileId(), chunk.id(), label, Optional.of(chunk.text())));
        }
        return out;
    }

    private void bumpQuota(UserId user, int tokens) {
        YearMonth period = YearMonth.now();
        QuotaUsage cur = quotas.find(user, period).orElse(new QuotaUsage(user, period, 0, 0, 0, 0));
        quotas.save(new QuotaUsage(user, period, cur.questionsAsked() + 1,
            cur.filesUploaded(), cur.bytesStored(), cur.tokensConsumed() + tokens));
    }

    private String modeLabel(Mode m) {
        return switch (m) {
            case GROUNDED -> "grounded";
            case FALLBACK -> "fallback";
            case REGULAR -> "regular";
        };
    }

    private Chat ownedChat(UserId actingUser, ChatId chatId) {
        Chat chat = chats.findById(chatId).orElseThrow(() -> new NotFoundError("chat not found"));
        if (!chat.ownerId().equals(actingUser)) throw new Forbidden("not your chat");
        return chat;
    }
}
