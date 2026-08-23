package com.tassist.application.chat;

import com.tassist.domain.port.out.LLMClient;
import com.tassist.domain.port.out.LLMClient.LlmMessage;
import com.tassist.domain.port.out.LLMClient.LlmRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Generates a short, human chat title from the first user message via a cheap one-shot LLM call.
 * Never throws to the caller: on any failure it falls back to a truncation of the message, so
 * titling can never break the chat flow. The message text here is the user's own question — no
 * document content is sent (consistent with the RAG invariant for retrieval, this is metadata only).
 */
@Service
public class ChatTitleService {
    private static final Logger log = LoggerFactory.getLogger(ChatTitleService.class);
    private static final int MAX_TITLE = 60;

    private static final String SYSTEM = """
        You generate a short title for a chat based on the user's first message.
        Rules: 3-6 words, Title Case, no quotes, no trailing punctuation, no emoji.
        Capture the topic. Reply with ONLY the title, nothing else.""";

    private final LLMClient llm;

    public ChatTitleService(LLMClient llm) { this.llm = llm; }

    /** Returns a clean title for the given first message. Always returns something usable. */
    public String titleFor(String firstMessage) {
        String fallback = truncate(firstMessage);
        if (firstMessage == null || firstMessage.isBlank()) return "New chat";
        try {
            var req = new LlmRequest(SYSTEM,
                List.of(new LlmMessage("user", firstMessage.strip())), List.of());
            String raw = llm.complete(req).content();
            String cleaned = clean(raw);
            return cleaned.isBlank() ? fallback : cleaned;
        } catch (Exception e) {
            log.warn("Title generation failed, using fallback: {}", e.getMessage());
            return fallback;
        }
    }

    private static String clean(String s) {
        if (s == null) return "";
        String t = s.strip().replaceAll("^[\"'`]+|[\"'`]+$", "").strip();
        t = t.replaceAll("[.!?,;:]+$", "").strip();
        if (t.length() > MAX_TITLE) t = t.substring(0, MAX_TITLE).strip();
        return t;
    }

    private static String truncate(String s) {
        if (s == null || s.isBlank()) return "New chat";
        String t = s.strip().replaceAll("\\s+", " ");
        return t.length() <= 40 ? t : t.substring(0, 39).strip() + "…";
    }
}
