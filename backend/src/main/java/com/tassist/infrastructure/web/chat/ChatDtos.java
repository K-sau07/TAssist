package com.tassist.infrastructure.web.chat;

import com.tassist.domain.model.Chat;
import com.tassist.domain.model.Citation;
import com.tassist.domain.model.Message;
import java.util.List;

/** §12.4 chat request/response shapes. */
public final class ChatDtos {
    private ChatDtos() {}

    public record CreateChatRequest(String scope, String folderId) {}
    public record RenameChatRequest(String title) {}
    public record SendMessageRequest(String content) {}

    public record ChatView(String id, String scope, String folderId, String title,
                           String createdAt, String updatedAt) {
        public static ChatView of(Chat c) {
            return new ChatView(c.id().value().toString(), c.scope().name(),
                c.folderId().map(f -> f.value().toString()).orElse(null),
                c.title(), c.createdAt().toString(), c.updatedAt().toString());
        }
    }

    public record CitationView(String fileId, String chunkId, String label, String snippet) {
        public static CitationView of(Citation c) {
            return new CitationView(c.fileId().value().toString(), c.chunkId().value().toString(),
                c.displayLabel(), c.snippet().orElse(null));
        }
    }

    public record MessageView(String id, String role, String content,
                              List<CitationView> citations, List<String> mentionedFiles, String createdAt) {
        public static MessageView of(Message m) {
            return new MessageView(m.id().value().toString(), m.role().name(), m.content(),
                m.citations().stream().map(CitationView::of).toList(),
                m.mentionedFiles().stream().map(f -> f.value().toString()).toList(),
                m.createdAt().toString());
        }
    }

    public record ChatDetailView(ChatView chat, List<MessageView> messages) {}

    public record SendMessageResponse(MessageView userMessage, MessageView assistantMessage,
                                      String mode, List<String> warnings) {}
}
