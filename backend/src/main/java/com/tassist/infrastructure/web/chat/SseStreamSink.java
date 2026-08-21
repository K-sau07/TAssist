package com.tassist.infrastructure.web.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tassist.application.chat.StreamSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Adapts the transport-agnostic {@link StreamSink} onto a Spring {@link SseEmitter} (§11.6).
 * Serializes each payload to JSON, writes named SSE events, and sends a `: ping` keep-alive
 * comment every 15 seconds until completion.
 */
public class SseStreamSink implements StreamSink {

    private static final Logger log = LoggerFactory.getLogger(SseStreamSink.class);

    private final SseEmitter emitter;
    private final ObjectMapper json;
    private final ScheduledFuture<?> keepAlive;

    public SseStreamSink(SseEmitter emitter, ObjectMapper json, ScheduledExecutorService pinger) {
        this.emitter = emitter;
        this.json = json;
        this.keepAlive = pinger.scheduleAtFixedRate(this::ping, 15, 15, TimeUnit.SECONDS);
    }

    @Override
    public void emit(String event, Map<String, Object> data) {
        try {
            emitter.send(SseEmitter.event().name(event).data(json.writeValueAsString(data)));
        } catch (IOException | IllegalStateException e) {
            log.debug("SSE emit failed (client likely disconnected): {}", e.toString());
        }
    }

    @Override
    public void complete() {
        keepAlive.cancel(false);
        try { emitter.complete(); } catch (RuntimeException ignored) { }
    }

    @Override
    public void fail(Throwable t) {
        keepAlive.cancel(false);
        try { emitter.completeWithError(t); } catch (RuntimeException ignored) { }
    }

    private void ping() {
        try {
            emitter.send(SseEmitter.event().comment("ping"));
        } catch (IOException | IllegalStateException e) {
            keepAlive.cancel(false);
        }
    }
}
