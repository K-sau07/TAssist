package com.tassist.application.channel;

import com.tassist.domain.error.Forbidden;
import com.tassist.domain.error.NotFoundError;
import com.tassist.domain.model.Chat;
import com.tassist.domain.model.Message;
import com.tassist.domain.model.Membership;
import com.tassist.domain.port.out.ChatRepository;
import com.tassist.domain.port.out.MessageRepository;
import com.tassist.domain.port.out.ChannelRepository;
import com.tassist.domain.port.out.MembershipRepository;
import com.tassist.domain.vo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Channel chat lifecycle (§12.6 visitor side). A channel chat is owned by the member who created it;
 * the channel owner never sees members' chats (only aggregated analytics). Access requires an
 * APPROVED membership (or being the channel owner).
 */
@Service
public class ChannelChatService {

    private static final Logger log = LoggerFactory.getLogger(ChannelChatService.class);
    private static final String DEFAULT_TITLE = "New chat";

    private final ChatRepository chats;
    private final MessageRepository messages;
    private final ChannelRepository channels;
    private final MembershipRepository memberships;

    public ChannelChatService(ChatRepository chats, MessageRepository messages,
                              ChannelRepository channels, MembershipRepository memberships) {
        this.chats = chats;
        this.messages = messages;
        this.channels = channels;
        this.memberships = memberships;
    }

    /** Verify the user may participate in this channel (APPROVED member or the owner). */
    public void requireAccess(UserId user, ChannelId channelId) {
        var channel = channels.findById(channelId)
            .orElseThrow(() -> new NotFoundError("channel not found"));
        if (channel.ownerId().equals(user)) return;
        Membership m = memberships.findByChannelAndUser(channelId, user)
            .orElseThrow(() -> new Forbidden("you are not a member of this channel"));
        if (m.status() != MembershipStatus.APPROVED)
            throw new Forbidden("your membership is not approved (status " + m.status() + ")");
    }

    @Transactional
    public Chat create(UserId user, ChannelId channelId) {
        requireAccess(user, channelId);
        Instant now = Instant.now();
        Chat chat = new Chat(ChatId.newId(), user, ChatScope.CHANNEL, Optional.empty(),
            Optional.of(channelId), DEFAULT_TITLE, now, now);
        Chat saved = chats.save(chat);
        log.info("Channel chat created: {} channel={} member={}", saved.id().value(),
            channelId.value(), user.value());
        return saved;
    }

    public List<Chat> listMine(UserId user, ChannelId channelId) {
        requireAccess(user, channelId);
        return chats.findByOwner(user).stream()
            .filter(c -> c.scope() == ChatScope.CHANNEL && c.channelId().equals(Optional.of(channelId)))
            .toList();
    }

    public Chat getOwnedInChannel(UserId user, ChannelId channelId, ChatId chatId) {
        requireAccess(user, channelId);
        Chat chat = chats.findById(chatId).orElseThrow(() -> new NotFoundError("chat not found"));
        if (!chat.ownerId().equals(user)) throw new Forbidden("not your chat");
        if (!chat.channelId().equals(Optional.of(channelId)))
            throw new NotFoundError("chat does not belong to this channel");
        return chat;
    }

    public List<Message> messages(UserId user, ChannelId channelId, ChatId chatId) {
        getOwnedInChannel(user, channelId, chatId);
        return messages.findByChat(chatId);
    }
}
