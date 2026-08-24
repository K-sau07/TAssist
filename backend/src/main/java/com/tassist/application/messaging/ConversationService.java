package com.tassist.application.messaging;

import com.tassist.domain.error.Forbidden;
import com.tassist.domain.error.NotFoundError;
import com.tassist.domain.error.ValidationError;
import com.tassist.domain.model.Channel;
import com.tassist.domain.model.Conversation;
import com.tassist.domain.model.ConversationMessage;
import com.tassist.domain.model.User;
import com.tassist.domain.port.out.*;
import com.tassist.domain.vo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Channel human-messaging lifecycle (02_MESSAGING_SPEC §5-§7). Handles DM open-or-create, group
 * access, posting, soft-delete, read state, and listing — with the full §6 access matrix and §11
 * edge cases. AI-in-thread (@ai/@assist) is layered on top in M5; this service is human-message +
 * access core, and exposes hooks the AI layer calls.
 *
 * <p>Access model: {@code canParticipate(user, channel)} = user is the owner OR an APPROVED member.
 * Membership is checked live (never cached) so a ban/leave/kick revokes access on the next call.
 */
@Service
public class ConversationService {

    private static final Logger log = LoggerFactory.getLogger(ConversationService.class);
    static final int MAX_MESSAGE_CHARS = 4000;
    private static final int DEFAULT_PAGE = 50;

    private final ConversationRepository conversations;
    private final ConversationMessageRepository messages;
    private final ConversationReadRepository reads;
    private final ChannelRepository channels;
    private final MembershipRepository memberships;
    private final UserRepository users;
    private final ConversationEventBus events;

    public ConversationService(ConversationRepository conversations,
                               ConversationMessageRepository messages,
                               ConversationReadRepository reads,
                               ChannelRepository channels,
                               MembershipRepository memberships,
                               UserRepository users,
                               ConversationEventBus events) {
        this.conversations = conversations;
        this.messages = messages;
        this.reads = reads;
        this.channels = channels;
        this.memberships = memberships;
        this.users = users;
        this.events = events;
    }

    // ── access ──────────────────────────────────────────────────────────────

    private Channel channelOrThrow(ChannelId channelId) {
        return channels.findById(channelId).orElseThrow(() -> new NotFoundError("channel not found"));
    }

    /** True if user is the channel owner or an APPROVED member. */
    public boolean canParticipate(UserId user, Channel channel) {
        if (channel.ownerId().equals(user)) return true;
        return memberships.findByChannelAndUser(channel.id(), user)
            .map(m -> m.status() == MembershipStatus.APPROVED).orElse(false);
    }

    private void requireParticipant(UserId user, Channel channel) {
        if (!canParticipate(user, channel))
            throw new Forbidden("you are not an approved participant of this channel");
    }

    /** Load a conversation and assert the caller may access it (DM party, or group participant). */
    private Conversation requireConversationAccess(UserId me, ChannelId channelId, ConversationId conversationId) {
        Channel channel = channelOrThrow(channelId);
        Conversation conv = conversations.findById(conversationId)
            .orElseThrow(() -> new NotFoundError("conversation not found"));
        if (!conv.channelId().equals(channelId))
            throw new NotFoundError("conversation does not belong to this channel");
        switch (conv.kind()) {
            case DM -> {
                if (!conv.hasParticipant(me))
                    throw new Forbidden("this is not your conversation");
                // §11.1: a DM freezes if a party loses channel access (ban/leave/kick).
                // Reading history is allowed; posting is blocked. We enforce "still allowed" here
                // for BOTH read and post — a banned user should not see the channel's DMs at all.
                if (!canParticipate(me, channel))
                    throw new Forbidden("your access to this channel has been revoked");
            }
            case GROUP -> {
                requireParticipant(me, channel);
                if (!channel.groupChatEnabled())
                    throw new Forbidden("the group room is disabled for this channel");
            }
        }
        return conv;
    }

    // ── DM open-or-create ───────────────────────────────────────────────────

    /** Open (or create) the DM between {@code me} and {@code target}. Idempotent (canonical pair). */
    @Transactional
    public Conversation openOrCreateDm(UserId me, ChannelId channelId, UserId target) {
        Channel channel = channelOrThrow(channelId);
        if (me.equals(target)) throw new ValidationError("cannot open a DM with yourself");
        requireParticipant(me, channel);
        // target must also be able to participate (approved member or owner)
        if (!canParticipate(target, channel))
            throw new ValidationError("the other user is not an approved participant of this channel");

        return conversations.findDm(channelId, me, target).orElseGet(() -> {
            Conversation created = conversations.save(
                Conversation.dm(ConversationId.newId(), channelId, me, target, Instant.now()));
            log.info("DM opened: {} channel={} between={} and={}",
                created.id().value(), channelId.value(), me.value(), target.value());
            return created;
        });
    }

    // ── group open-or-create ────────────────────────────────────────────────

    /** The channel's group room, creating it lazily on first access if enabled. */
    @Transactional
    public Conversation openGroup(UserId me, ChannelId channelId) {
        Channel channel = channelOrThrow(channelId);
        requireParticipant(me, channel);
        if (!channel.groupChatEnabled())
            throw new Forbidden("the group room is disabled for this channel");
        return conversations.findGroup(channelId).orElseGet(() ->
            conversations.save(Conversation.group(ConversationId.newId(), channelId, Instant.now())));
    }

    // ── listing ─────────────────────────────────────────────────────────────

    /** The caller's DM conversations in this channel, newest-updated first. */
    public List<Conversation> listMyDms(UserId me, ChannelId channelId) {
        Channel channel = channelOrThrow(channelId);
        requireParticipant(me, channel);
        return conversations.findDmsForUser(channelId, me);
    }

    /** Users the caller can DM: approved members + owner, excluding the caller. */
    public List<User> listParticipants(UserId me, ChannelId channelId) {
        Channel channel = channelOrThrow(channelId);
        requireParticipant(me, channel);
        java.util.LinkedHashMap<UserId, User> out = new java.util.LinkedHashMap<>();
        // owner first
        if (!channel.ownerId().equals(me))
            users.findById(channel.ownerId()).ifPresent(u -> out.put(u.id(), u));
        // approved members
        for (var m : memberships.findByChannelAndStatus(channelId, MembershipStatus.APPROVED)) {
            if (m.userId().equals(me)) continue;
            users.findById(m.userId()).ifPresent(u -> out.putIfAbsent(u.id(), u));
        }
        return List.copyOf(out.values());
    }

    // ── messages ────────────────────────────────────────────────────────────

    /** Post a human message. Returns the saved message. AI turns are triggered by the caller (M5). */
    @Transactional
    public ConversationMessage postHuman(UserId me, ChannelId channelId, ConversationId conversationId, String content) {
        Conversation conv = requireConversationAccess(me, channelId, conversationId);
        String trimmed = content == null ? "" : content.strip();
        if (trimmed.isEmpty()) throw new ValidationError("message content must not be empty");
        if (trimmed.length() > MAX_MESSAGE_CHARS)
            throw new ValidationError("message exceeds " + MAX_MESSAGE_CHARS + " characters");

        Instant now = Instant.now();
        ConversationMessage saved = messages.save(
            ConversationMessage.human(ConversationMessageId.newId(), conv.id(), me, trimmed, now));
        conversations.save(conv.touched(now)); // bump updatedAt for inbox sort
        // sender has implicitly read up to their own message
        reads.markRead(conv.id(), me, now);
        publishMessage(saved);
        return saved;
    }

    /** Persist an AI-authored message (called by the M5 AI layer after grounded generation). */
    @Transactional
    public ConversationMessage saveAiMessage(ConversationId conversationId, String content,
                                             List<com.tassist.domain.model.Citation> citations) {
        Conversation conv = conversations.findById(conversationId)
            .orElseThrow(() -> new NotFoundError("conversation not found"));
        Instant now = Instant.now();
        ConversationMessage saved = messages.save(
            ConversationMessage.ai(ConversationMessageId.newId(), conv.id(), content, citations, now));
        conversations.save(conv.touched(now));
        publishMessage(saved);
        return saved;
    }

    /** Page messages oldest-first. {@code before} = cursor (null → latest page). */
    public List<ConversationMessage> listMessages(UserId me, ChannelId channelId,
                                                  ConversationId conversationId, Instant before, Integer limit) {
        requireConversationAccess(me, channelId, conversationId);
        int lim = (limit == null || limit <= 0) ? DEFAULT_PAGE : Math.min(limit, 100);
        return messages.findByConversation(conversationId, before, lim);
    }

    /** Soft-delete a message. Sender may delete own anywhere; owner may delete any GROUP message. */
    @Transactional
    public void deleteMessage(UserId me, ChannelId channelId, ConversationId conversationId,
                              ConversationMessageId messageId) {
        Channel channel = channelOrThrow(channelId);
        Conversation conv = requireConversationAccess(me, channelId, conversationId);
        ConversationMessage msg = messages.findById(messageId)
            .orElseThrow(() -> new NotFoundError("message not found"));
        if (!msg.conversationId().equals(conversationId))
            throw new NotFoundError("message does not belong to this conversation");

        boolean isOwnMessage = msg.senderId().map(me::equals).orElse(false);
        boolean isGroupOwnerModeration =
            conv.kind() == ConversationKind.GROUP && channel.ownerId().equals(me);
        if (!isOwnMessage && !isGroupOwnerModeration)
            throw new Forbidden("you can only delete your own messages");

        if (!msg.isDeleted()) {
            messages.save(msg.deleted(Instant.now()));
            events.publish(conversationId, "deleted",
                java.util.Map.of("messageId", messageId.value().toString()));
        }
    }

    // ── group toggle (owner only) ─────────────────────────────────────────────

    /** Enable/disable the channel's group room. Owner only (02_MESSAGING_SPEC §2.2). */
    @Transactional
    public void setGroupEnabled(UserId me, ChannelId channelId, boolean enabled) {
        Channel channel = channelOrThrow(channelId);
        if (!channel.ownerId().equals(me))
            throw new Forbidden("only the channel owner can change the group room setting");
        channels.save(channel.withGroupChatEnabled(enabled));
    }

    // ── read state ──────────────────────────────────────────────────────────

    /** Mark the conversation read for the caller up to {@code upTo} (or now). Max-wins. */
    @Transactional
    public void markRead(UserId me, ChannelId channelId, ConversationId conversationId, Instant upTo) {
        requireConversationAccess(me, channelId, conversationId);
        Instant at = upTo == null ? Instant.now() : upTo;
        reads.markRead(conversationId, me, at);
        events.publish(conversationId, "read",
            java.util.Map.of("userId", me.value().toString(), "at", at.toString()));
    }

    /** Unread count for {@code me} in a conversation (messages after last-read, not own, not deleted). */
    public long unreadCount(UserId me, ConversationId conversationId) {
        Instant since = reads.findLastRead(conversationId, me).orElse(Instant.EPOCH);
        return messages.countUnread(conversationId, since, me);
    }

    /** Convenience: load a conversation with access enforced (for view assembly). */
    public Conversation getAccessible(UserId me, ChannelId channelId, ConversationId conversationId) {
        return requireConversationAccess(me, channelId, conversationId);
    }

    /** Latest message in a conversation (for inbox previews); access already enforced by list. */
    public Optional<ConversationMessage> latestMessage(ConversationId conversationId) {
        return messages.findLatest(conversationId);
    }

    /** Fan out a new message to all subscribers of its conversation (realtime, §8). */
    private void publishMessage(ConversationMessage m) {
        java.util.Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("messageId", m.id().value().toString());
        payload.put("conversationId", m.conversationId().value().toString());
        payload.put("senderKind", m.senderKind().name());
        payload.put("senderId", m.senderId().map(u -> u.value().toString()).orElse(null));
        payload.put("content", m.content());
        payload.put("createdAt", m.createdAt().toString());
        events.publish(m.conversationId(), "message", payload);
    }
}
