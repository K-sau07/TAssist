package com.tassist.infrastructure.web.channel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tassist.application.channel.ChannelChatService;
import com.tassist.application.chat.ChatStreamService;
import com.tassist.domain.port.in.QuotaUseCase;
import com.tassist.domain.error.Unauthenticated;
import com.tassist.domain.error.ValidationError;
import com.tassist.domain.model.Chat;
import com.tassist.domain.vo.ChannelId;
import com.tassist.domain.vo.ChatId;
import com.tassist.domain.vo.UserId;
import com.tassist.infrastructure.web.chat.ChatDtos.SendMessageRequest;
import com.tassist.infrastructure.web.chat.SseStreamSink;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/** §12.6 channel chat endpoints (visitor side). Access requires an APPROVED membership. */
@RestController
@RequestMapping("/api/channels/{channelId}/chats")
public class ChannelChatController {

    private final ChannelChatService channelChats;
    private final ChatStreamService streamer;
    private final QuotaUseCase quota;
    private final ObjectMapper json = new ObjectMapper();
    private final ExecutorService streamPool = Executors.newCachedThreadPool();
    private final ScheduledExecutorService pinger = Executors.newScheduledThreadPool(2);

    public ChannelChatController(ChannelChatService channelChats, ChatStreamService streamer, QuotaUseCase quota) {
        this.channelChats = channelChats;
        this.streamer = streamer;
        this.quota = quota;
    }

    @GetMapping
    public List<ChatSummary> list(@PathVariable String channelId, Authentication auth) {
        UserId user = principal(auth);
        return channelChats.listMine(user, channelId(channelId)).stream().map(ChatSummary::of).toList();
    }

    @PostMapping
    public ResponseEntity<ChatSummary> create(@PathVariable String channelId, Authentication auth) {
        UserId user = principal(auth);
        Chat c = channelChats.create(user, channelId(channelId));
        return ResponseEntity.status(HttpStatus.CREATED).body(ChatSummary.of(c));
    }

    @GetMapping("/{chatId}")
    public ChatWithMessages get(@PathVariable String channelId, @PathVariable String chatId,
                                Authentication auth) {
        UserId user = principal(auth);
        Chat c = channelChats.getOwnedInChannel(user, channelId(channelId), chatId(chatId));
        var msgs = channelChats.messages(user, channelId(channelId), chatId(chatId)).stream()
            .map(m -> new MsgView(m.id().value().toString(), m.role().name(), m.content(), m.createdAt()))
            .toList();
        return new ChatWithMessages(ChatSummary.of(c), msgs);
    }

    @PostMapping("/{chatId}/messages/stream")
    public SseEmitter stream(@PathVariable String channelId, @PathVariable String chatId,
                             @RequestBody SendMessageRequest req, Authentication auth) {
        UserId user = principal(auth);
        if (req == null || req.content() == null) throw new ValidationError("content is required");
        ChannelId cid = channelId(channelId);
        ChatId chId = chatId(chatId);
        // authorize: APPROVED member (or owner) AND owns this chat within this channel
        channelChats.getOwnedInChannel(user, cid, chId);
        quota.checkQuestionAllowed(user); // §16.2 pre-stream clean 429
        SseEmitter emitter = new SseEmitter(0L);
        SseStreamSink sink = new SseStreamSink(emitter, json, pinger);
        streamPool.submit(() -> streamer.streamMessage(user, chId, req.content(), sink));
        return emitter;
    }

    // ---- DTOs ----
    public record ChatSummary(String id, String channelId, String title, Instant createdAt) {
        static ChatSummary of(Chat c) {
            return new ChatSummary(c.id().value().toString(),
                c.channelId().map(x -> x.value().toString()).orElse(null), c.title(), c.createdAt());
        }
    }
    public record MsgView(String id, String role, String content, Instant createdAt) {}
    public record ChatWithMessages(ChatSummary chat, List<MsgView> messages) {}

    // ---- helpers ----
    private UserId principal(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof UserId userId))
            throw new Unauthenticated("authentication required");
        return userId;
    }
    private ChannelId channelId(String raw) {
        try { return ChannelId.of(raw); } catch (Exception e) { throw new ValidationError("invalid channelId"); }
    }
    private ChatId chatId(String raw) {
        try { return ChatId.of(raw); } catch (Exception e) { throw new ValidationError("invalid chatId"); }
    }
}
