package com.tassist.infrastructure.web.chat;

import com.tassist.domain.error.Unauthenticated;
import com.tassist.domain.error.ValidationError;
import com.tassist.domain.model.Chat;
import com.tassist.domain.port.in.ChatUseCase;
import com.tassist.domain.port.in.ChatUseCase.CreateChatCommand;
import com.tassist.domain.port.in.ChatUseCase.SendResult;
import com.tassist.application.chat.ChatStreamService;
import com.tassist.domain.port.in.QuotaUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tassist.domain.vo.ChatId;
import com.tassist.domain.vo.ChatScope;
import com.tassist.domain.vo.FolderId;
import com.tassist.domain.vo.UserId;
import com.tassist.infrastructure.web.chat.ChatDtos.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/** §12.4 private-library chat endpoints (non-streaming). /messages/stream is Step 11. */
@RestController
@RequestMapping("/api/chats")
public class ChatController {

    private final ChatUseCase chats;
    private final ChatStreamService streamer;
    private final QuotaUseCase quota;
    private final ObjectMapper json = new ObjectMapper();
    private final ExecutorService streamPool = Executors.newCachedThreadPool();
    private final ScheduledExecutorService pinger = Executors.newScheduledThreadPool(2);

    public ChatController(ChatUseCase chats, ChatStreamService streamer, QuotaUseCase quota) {
        this.chats = chats;
        this.streamer = streamer;
        this.quota = quota;
    }

    @GetMapping
    public ResponseEntity<List<ChatView>> list(Authentication auth) {
        return ResponseEntity.ok(chats.list(principal(auth)).stream().map(ChatView::of).toList());
    }

    @PostMapping
    public ResponseEntity<ChatView> create(@RequestBody CreateChatRequest req, Authentication auth) {
        UserId user = principal(auth);
        if (req == null || req.scope() == null) throw new ValidationError("scope is required");
        ChatScope scope = parseScope(req.scope());
        Optional<FolderId> folderId = req.folderId() == null || req.folderId().isBlank()
            ? Optional.empty() : Optional.of(folderId(req.folderId()));
        Chat c = chats.create(user, new CreateChatCommand(scope, folderId));
        return ResponseEntity.status(HttpStatus.CREATED)
            .location(URI.create("/api/chats/" + c.id().value()))
            .body(ChatView.of(c));
    }

    @GetMapping("/{chatId}")
    public ResponseEntity<ChatDetailView> get(@PathVariable String chatId, Authentication auth) {
        UserId user = principal(auth);
        ChatId id = chatId(chatId);
        Chat c = chats.get(user, id);
        var msgs = chats.getMessages(user, id).stream().map(MessageView::of).toList();
        return ResponseEntity.ok(new ChatDetailView(ChatView.of(c), msgs));
    }

    @PatchMapping("/{chatId}")
    public ResponseEntity<ChatView> rename(@PathVariable String chatId,
                                           @RequestBody RenameChatRequest req, Authentication auth) {
        UserId user = principal(auth);
        if (req == null || req.title() == null) throw new ValidationError("title is required");
        return ResponseEntity.ok(ChatView.of(chats.rename(user, chatId(chatId), req.title())));
    }

    @DeleteMapping("/{chatId}")
    public ResponseEntity<Void> delete(@PathVariable String chatId, Authentication auth) {
        chats.delete(principal(auth), chatId(chatId));
        return ResponseEntity.noContent().build();
    }

    /** Non-streaming send (Step 10). SSE variant is /messages/stream in Step 11. */
    @PostMapping("/{chatId}/messages")
    public ResponseEntity<SendMessageResponse> send(@PathVariable String chatId,
                                                    @RequestBody SendMessageRequest req, Authentication auth) {
        UserId user = principal(auth);
        if (req == null || req.content() == null) throw new ValidationError("content is required");
        SendResult r = chats.sendMessage(user, chatId(chatId), req.content());
        return ResponseEntity.status(HttpStatus.CREATED).body(new SendMessageResponse(
            MessageView.of(r.userMessage()), MessageView.of(r.assistantMessage()),
            r.mode(), r.warnings()));
    }

    /** §12.4 §11.6 streaming send. Returns an SSE stream of start/sources/token/citation/done events. */
    @PostMapping("/{chatId}/messages/stream")
    public SseEmitter stream(@PathVariable String chatId,
                             @RequestBody SendMessageRequest req, Authentication auth) {
        UserId user = principal(auth);
        if (req == null || req.content() == null) throw new ValidationError("content is required");
        ChatId cid = chatId(chatId);
        quota.checkQuestionAllowed(user); // §16.2 pre-stream: clean 429 before headers are sent
        SseEmitter emitter = new SseEmitter(0L); // no timeout; keep-alive pings hold it open
        SseStreamSink sink = new SseStreamSink(emitter, json, pinger);
        streamPool.submit(() -> streamer.streamMessage(user, cid, req.content(), sink));
        return emitter;
    }

    // --- helpers ---
    private UserId principal(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof UserId userId))
            throw new Unauthenticated("authentication required");
        return userId;
    }
    private ChatScope parseScope(String raw) {
        try { return ChatScope.valueOf(raw.trim().toUpperCase()); }
        catch (IllegalArgumentException e) { throw new ValidationError("invalid scope: " + raw); }
    }
    private ChatId chatId(String raw) {
        try { return ChatId.of(raw); } catch (IllegalArgumentException e) { throw new ValidationError("invalid chatId"); }
    }
    private FolderId folderId(String raw) {
        try { return FolderId.of(raw); } catch (IllegalArgumentException e) { throw new ValidationError("invalid folderId"); }
    }
}
