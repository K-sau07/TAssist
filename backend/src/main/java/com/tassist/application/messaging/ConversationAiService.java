package com.tassist.application.messaging;

import com.tassist.application.generation.CitationLabeler;
import com.tassist.application.generation.GenerationService;
import com.tassist.domain.model.Chunk;
import com.tassist.domain.model.Citation;
import com.tassist.domain.model.ConversationMessage;
import com.tassist.domain.model.File;
import com.tassist.domain.port.in.QuotaUseCase;
import com.tassist.domain.port.in.RetrievalUseCase;
import com.tassist.domain.port.in.RetrievalUseCase.RetrievalQuery;
import com.tassist.domain.port.in.RetrievalUseCase.RetrievalResult;
import com.tassist.domain.port.in.RetrievalUseCase.Scope;
import com.tassist.domain.port.in.RetrievalUseCase.TextHit;
import com.tassist.domain.port.out.ChannelFileRepository;
import com.tassist.domain.port.out.FileRepository;
import com.tassist.domain.vo.ChannelId;
import com.tassist.domain.vo.ConversationId;
import com.tassist.domain.vo.FileId;
import com.tassist.domain.vo.FileType;
import com.tassist.domain.vo.UserId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI-in-thread (02_MESSAGING_SPEC §7). When a human message tags {@code @ai} or {@code @assist},
 * this runs the SAME channel RAG path used by channel AI chat — retrieval scoped to the channel's
 * attached files, grounded generation, and citations by owner display_label (§7.5, never real
 * filenames) — then persists the answer as an AI message in the conversation, visible to all
 * participants. No hits → grounded-fallback, never a hallucination.
 */
@Service
public class ConversationAiService {

    private static final Logger log = LoggerFactory.getLogger(ConversationAiService.class);

    /** Standalone @ai or @assist token (case-insensitive), not part of a longer word/email. */
    private static final Pattern TRIGGER = Pattern.compile("(?i)(?<![\\w@])@(ai|assist)\\b");
    /** [Sn] citation markers emitted by grounded generation. */
    private static final Pattern CITE = Pattern.compile("\\[S(\\d+)\\]");

    private final ConversationService conversations;
    private final RetrievalUseCase retrieval;
    private final GenerationService generation;
    private final ChannelFileRepository channelFiles;
    private final FileRepository files;
    private final QuotaUseCase quota;

    public ConversationAiService(ConversationService conversations,
                                 RetrievalUseCase retrieval,
                                 GenerationService generation,
                                 ChannelFileRepository channelFiles,
                                 FileRepository files,
                                 QuotaUseCase quota) {
        this.conversations = conversations;
        this.retrieval = retrieval;
        this.generation = generation;
        this.channelFiles = channelFiles;
        this.files = files;
        this.quota = quota;
    }

    /** True if the text tags the AI (@ai or @assist). */
    public static boolean mentionsAi(String content) {
        return content != null && TRIGGER.matcher(content).find();
    }

    /** Remove the @ai/@assist trigger token from the query text so it doesn't pollute retrieval. */
    static String stripTrigger(String content) {
        if (content == null) return "";
        return TRIGGER.matcher(content).replaceAll(" ").replaceAll("\\s+", " ").strip();
    }

    /**
     * Run a grounded AI turn for a human message that tagged the AI. Returns the persisted AI
     * message, or empty if the message didn't actually tag the AI. Never throws on generation
     * failure — a failed AI turn must not lose the human message (that was already saved upstream).
     */
    public Optional<ConversationMessage> maybeRespond(UserId invoker, ChannelId channelId,
                                                      ConversationId conversationId, String humanContent) {
        if (!mentionsAi(humanContent)) return Optional.empty();
        String question = stripTrigger(humanContent);
        if (question.isBlank()) {
            // tagged the AI but asked nothing — a gentle nudge, still grounded-safe
            return Optional.of(conversations.saveAiMessage(conversationId,
                "You mentioned me — ask a question about this channel's material and I'll answer from it.",
                List.of()));
        }
        try {
            RetrievalResult retrieved = retrieval.retrieve(new RetrievalQuery(
                invoker, question, Scope.CHANNEL, Optional.empty(), Optional.of(channelId), List.of()));

            var outcome = generation.generate(question, retrieved, /*regularScope*/ false);

            List<Citation> citations = buildChannelCitations(outcome.answer(), retrieved.textHits(), channelId);
            ConversationMessage aiMsg = conversations.saveAiMessage(conversationId, outcome.answer(), citations);

            quota.recordQuestion(invoker, (long) outcome.inputTokens() + outcome.outputTokens());
            log.info("AI-in-thread: conv={} channel={} invoker={} mode={} citations={}",
                conversationId.value(), channelId.value(), invoker.value(), outcome.mode(), citations.size());
            return Optional.of(aiMsg);
        } catch (RuntimeException e) {
            // Never lose the human turn; surface a clean inline note instead of failing the request.
            log.error("AI-in-thread failed: conv={} err={}", conversationId.value(), e.toString());
            return Optional.of(conversations.saveAiMessage(conversationId,
                "I couldn't answer that just now. Please try again in a moment.", List.of()));
        }
    }

    /** [Sn] markers → citations labelled by the channel's owner display_label (§7.5). */
    private List<Citation> buildChannelCitations(String answer, List<TextHit> hits, ChannelId channelId) {
        if (answer == null || hits.isEmpty()) return List.of();
        Map<FileId, String> labelByFile = new HashMap<>();
        for (var cf : channelFiles.findByChannel(channelId)) labelByFile.put(cf.fileId(), cf.displayLabel());

        Set<Integer> cited = new LinkedHashSet<>();
        Matcher m = CITE.matcher(answer);
        while (m.find()) {
            int n = Integer.parseInt(m.group(1));
            if (n >= 1 && n <= hits.size()) cited.add(n);
        }
        List<Citation> out = new ArrayList<>(cited.size());
        for (int n : cited) {
            Chunk chunk = hits.get(n - 1).chunk();
            String label = labelByFile.get(chunk.fileId());
            if (label == null) {
                // fall back to a generic label — never leak the real filename in a channel (§7.5)
                File f = files.findById(chunk.fileId()).orElse(null);
                label = f != null
                    ? CitationLabeler.label("source", f.type() == null ? FileType.TXT : f.type(), chunk.metadata())
                    : "source";
            }
            out.add(new Citation(chunk.fileId(), chunk.id(), label, Optional.of(chunk.text())));
        }
        return out;
    }
}
