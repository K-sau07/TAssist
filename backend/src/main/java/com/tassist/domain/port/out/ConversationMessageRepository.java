package com.tassist.domain.port.out;

import com.tassist.domain.model.ConversationMessage;
import com.tassist.domain.vo.ConversationId;
import com.tassist.domain.vo.ConversationMessageId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Persistence port for {@link ConversationMessage} (02_MESSAGING_SPEC §5). */
public interface ConversationMessageRepository {
    ConversationMessage save(ConversationMessage message);
    Optional<ConversationMessage> findById(ConversationMessageId id);

    /** Messages in a conversation, oldest-first. {@code before} pages backwards (exclusive); null = latest page. */
    List<ConversationMessage> findByConversation(ConversationId conversationId, Instant before, int limit);

    /** Latest non-deleted message in a conversation (for inbox previews), if any. */
    Optional<ConversationMessage> findLatest(ConversationId conversationId);

    /** Count of messages in {@code conversation} created after {@code after}, excluding {@code excludeSender}'s own and deleted. */
    long countUnread(ConversationId conversationId, Instant after, com.tassist.domain.vo.UserId excludeSender);
}
