package com.tassist.infrastructure.web.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tassist.application.messaging.ConversationAiService;
import com.tassist.application.messaging.ConversationEventBus;
import com.tassist.application.messaging.ConversationService;
import com.tassist.domain.error.Unauthenticated;
import com.tassist.domain.error.ValidationError;
import com.tassist.domain.model.Channel;
import com.tassist.domain.model.Conversation;
import com.tassist.domain.model.ConversationMessage;
import com.tassist.domain.model.User;
import com.tassist.domain.port.out.ChannelRepository;
import com.tassist.domain.port.out.UserRepository;
import com.tassist.domain.vo.*;
import com.tassist.infrastructure.web.messaging.MessagingDtos.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledFuture;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Channel human-messaging endpoints (02_MESSAGING_SPEC §9). DM + group conversations, messages,
 * read state. AI-in-thread (@ai/@assist) is composed here: post the human message, then run the
 * grounded AI turn. Realtime SSE fan-out is added in M7.
 */
@RestController
@RequestMapping("/api/channels/{channelId}")
public class ConversationController {

    private final ConversationService conversations;
    private final ConversationAiService ai;
    private final ConversationEventBus events;
    private final ChannelRepository channels;
    private final UserRepository users;
    private final ObjectMapper json = new ObjectMapper();
    private final ScheduledExecutorService pinger = Executors.newScheduledThreadPool(2);

    public ConversationController(ConversationService conversations, ConversationAiService ai,
                                  ConversationEventBus events, ChannelRepository channels, UserRepository users) {
        this.conversations = conversations;
        this.ai = ai;
        this.events = events;
        this.channels = channels;
        this.users = users;
    }

    // ── participants ──
    @GetMapping("/participants")
    public List<ParticipantView> participants(@PathVariable String channelId, Authentication auth) {
        UserId me = principal(auth);
        ChannelId cid = channelId(channelId);
        UserId ownerId = channelOwner(cid);
        return conversations.listParticipants(me, cid).stream()
            .map(u -> ParticipantView.of(u, u.id().equals(ownerId)))
            .toList();
    }

    // ── DM open-or-create ──
    @PostMapping("/dm")
    public ResponseEntity<ConversationView> openDm(@PathVariable String channelId,
                                                   @RequestBody OpenDmRequest req, Authentication auth) {
        UserId me = principal(auth);
        if (req == null || req.targetUserId() == null || req.targetUserId().isBlank())
            throw new ValidationError("targetUserId is required");
        UserId target = userId(req.targetUserId());
        Conversation c = conversations.openOrCreateDm(me, channelId(channelId), target);
        return ResponseEntity.status(HttpStatus.CREATED).body(toView(c, me));
    }

    // ── my DM inbox ──
    @GetMapping("/dms")
    public List<ConversationView> myDms(@PathVariable String channelId, Authentication auth) {
        UserId me = principal(auth);
        return conversations.listMyDms(me, channelId(channelId)).stream()
            .map(c -> toView(c, me)).toList();
    }

    // ── group ──
    @GetMapping("/group")
    public ConversationView group(@PathVariable String channelId, Authentication auth) {
        UserId me = principal(auth);
        Conversation g = conversations.openGroup(me, channelId(channelId));
        return toView(g, me);
    }

    @PutMapping("/group/enabled")
    public ResponseEntity<Void> toggleGroup(@PathVariable String channelId,
                                            @RequestBody GroupEnabledRequest req, Authentication auth) {
        UserId me = principal(auth);
        if (req == null || req.enabled() == null) throw new ValidationError("enabled is required");
        conversations.setGroupEnabled(me, channelId(channelId), req.enabled());
        return ResponseEntity.noContent().build();
    }

    // ── messages ──
    @GetMapping("/conversations/{conversationId}/messages")
    public List<MessageView> messages(@PathVariable String channelId, @PathVariable String conversationId,
                                      @RequestParam(required = false) Instant before,
                                      @RequestParam(required = false) Integer limit,
                                      Authentication auth) {
        UserId me = principal(auth);
        List<ConversationMessage> msgs = conversations.listMessages(
            me, channelId(channelId), conversationId(conversationId), before, limit);
        return renderMessages(msgs);
    }

    @PostMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<PostMessageResponse> post(@PathVariable String channelId,
                                                    @PathVariable String conversationId,
                                                    @RequestBody PostMessageRequest req, Authentication auth) {
        UserId me = principal(auth);
        if (req == null || req.content() == null) throw new ValidationError("content is required");
        ChannelId cid = channelId(channelId);
        ConversationId conv = conversationId(conversationId);

        ConversationMessage human = conversations.postHuman(me, cid, conv, req.content());
        MessageView humanView = MessageView.of(human, senderFor(human));

        // AI-in-thread: if the message tagged @ai/@assist, run the grounded turn now.
        MessageView aiView = null;
        Optional<ConversationMessage> aiReply = ai.maybeRespond(me, cid, conv, req.content());
        if (aiReply.isPresent()) aiView = MessageView.of(aiReply.get(), null);

        return ResponseEntity.status(HttpStatus.CREATED).body(new PostMessageResponse(humanView, aiView));
    }

    @PostMapping("/conversations/{conversationId}/read")
    public ResponseEntity<Void> markRead(@PathVariable String channelId, @PathVariable String conversationId,
                                         @RequestBody(required = false) MarkReadRequest req, Authentication auth) {
        UserId me = principal(auth);
        Instant upTo = req == null ? null : req.upTo();
        conversations.markRead(me, channelId(channelId), conversationId(conversationId), upTo);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/conversations/{conversationId}/messages/{messageId}")
    public ResponseEntity<Void> deleteMessage(@PathVariable String channelId, @PathVariable String conversationId,
                                              @PathVariable String messageId, Authentication auth) {
        UserId me = principal(auth);
        conversations.deleteMessage(me, channelId(channelId), conversationId(conversationId), messageId(messageId));
        return ResponseEntity.noContent().build();
    }

    // ── realtime SSE stream ──
    @GetMapping("/conversations/{conversationId}/stream")
    public SseEmitter stream(@PathVariable String channelId, @PathVariable String conversationId,
                             Authentication auth) {
        UserId me = principal(auth);
        ChannelId cid = channelId(channelId);
        ConversationId conv = conversationId(conversationId);
        // access enforced up front — throws 403/404 before we open the stream
        conversations.getAccessible(me, cid, conv);

        SseEmitter emitter = new SseEmitter(0L); // no timeout; keep-alive pings hold it open
        ConversationEventBus.Subscription sub = events.subscribe(conv, (event, payload) -> {
            try {
                emitter.send(SseEmitter.event().name(event).data(json.writeValueAsString(payload)));
            } catch (IOException | IllegalStateException e) {
                emitter.completeWithError(e); // triggers onError → cleanup
            }
        });
        ScheduledFuture<?> keepAlive = pinger.scheduleAtFixedRate(() -> {
            try { emitter.send(SseEmitter.event().comment("ping")); }
            catch (IOException | IllegalStateException e) { emitter.complete(); }
        }, 15, 15, TimeUnit.SECONDS);

        Runnable cleanup = () -> { sub.close(); keepAlive.cancel(false); };
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(t -> cleanup.run());
        return emitter;
    }

    // ── view assembly ──

    private ConversationView toView(Conversation c, UserId me) {
        ParticipantView other = null;
        if (c.kind() == ConversationKind.DM) {
            UserId ownerId = channelOwnerCached(c);
            other = c.otherParticipant(me)
                .flatMap(users::findById)
                .map(u -> ParticipantView.of(u, u.id().equals(ownerId)))
                .orElse(null);
        }
        String preview = conversations.latestMessage(c.id())
            .map(m -> m.isDeleted() ? "message deleted" : truncate(m.content()))
            .orElse(null);
        long unread = conversations.unreadCount(me, c.id());
        return new ConversationView(
            c.id().value().toString(), c.channelId().value().toString(), c.kind().name(),
            other, preview, unread, c.updatedAt());
    }

    private final Map<String, UserId> ownerCache = new HashMap<>();
    private UserId channelOwnerCached(Conversation c) {
        return ownerCache.computeIfAbsent(c.channelId().value().toString(), k -> channelOwner(c.channelId()));
    }

    private List<MessageView> renderMessages(List<ConversationMessage> msgs) {
        // batch-resolve human sender names
        Map<UserId, User> cache = new HashMap<>();
        for (ConversationMessage m : msgs) {
            m.senderId().ifPresent(sid -> cache.computeIfAbsent(sid, id -> users.findById(id).orElse(null)));
        }
        return msgs.stream().map(m -> {
            MessageSender sender = m.senderId().map(cache::get)
                .map(u -> u == null ? new MessageSender(null, "former member")
                                    : new MessageSender(u.id().value().toString(), u.displayName()))
                .orElse(null);
            return MessageView.of(m, sender);
        }).toList();
    }

    private MessageSender senderFor(ConversationMessage m) {
        return m.senderId().flatMap(users::findById)
            .map(u -> new MessageSender(u.id().value().toString(), u.displayName()))
            .orElse(null);
    }

    private static String truncate(String s) {
        if (s == null) return null;
        String t = s.strip();
        return t.length() <= 80 ? t : t.substring(0, 80) + "…";
    }

    // ── helpers ──
    private UserId channelOwner(ChannelId cid) {
        Channel c = channels.findById(cid)
            .orElseThrow(() -> new com.tassist.domain.error.NotFoundError("channel not found"));
        return c.ownerId();
    }
    private UserId principal(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof UserId userId))
            throw new Unauthenticated("authentication required");
        return userId;
    }
    private ChannelId channelId(String raw) {
        try { return ChannelId.of(raw); } catch (Exception e) { throw new ValidationError("invalid channelId"); }
    }
    private ConversationId conversationId(String raw) {
        try { return ConversationId.of(raw); } catch (Exception e) { throw new ValidationError("invalid conversationId"); }
    }
    private ConversationMessageId messageId(String raw) {
        try { return ConversationMessageId.of(raw); } catch (Exception e) { throw new ValidationError("invalid messageId"); }
    }
    private UserId userId(String raw) {
        try { return UserId.of(raw); } catch (Exception e) { throw new ValidationError("invalid targetUserId"); }
    }
}
