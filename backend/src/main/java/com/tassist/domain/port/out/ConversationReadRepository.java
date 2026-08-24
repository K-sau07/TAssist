package com.tassist.domain.port.out;

import com.tassist.domain.vo.ConversationId;
import com.tassist.domain.vo.UserId;
import java.time.Instant;
import java.util.Optional;

/** Persistence port for per-user read state on a conversation (02_MESSAGING_SPEC §5.4). */
public interface ConversationReadRepository {
    /** The user's last-read timestamp for a conversation, if they've ever read it. */
    Optional<Instant> findLastRead(ConversationId conversationId, UserId user);

    /** Upsert the user's last-read timestamp (max-wins across devices). */
    void markRead(ConversationId conversationId, UserId user, Instant at);
}
