package com.tassist.domain.port.out;

import com.tassist.domain.model.Conversation;
import com.tassist.domain.vo.ChannelId;
import com.tassist.domain.vo.ConversationId;
import com.tassist.domain.vo.UserId;
import java.util.List;
import java.util.Optional;

/** Persistence port for {@link Conversation} (02_MESSAGING_SPEC §5). */
public interface ConversationRepository {
    Conversation save(Conversation conversation);
    Optional<Conversation> findById(ConversationId id);

    /** The existing DM between two users in a channel, if any (participant order-independent). */
    Optional<Conversation> findDm(ChannelId channelId, UserId x, UserId y);

    /** The channel's group room, if it exists. */
    Optional<Conversation> findGroup(ChannelId channelId);

    /** All DM conversations in a channel where {@code user} is a participant, newest-updated first. */
    List<Conversation> findDmsForUser(ChannelId channelId, UserId user);

    void delete(ConversationId id);
}
