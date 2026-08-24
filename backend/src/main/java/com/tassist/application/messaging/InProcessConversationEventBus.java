package com.tassist.application.messaging;

import com.tassist.domain.vo.ConversationId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Single-instance in-process {@link ConversationEventBus}. Holds a set of listeners per conversation
 * in a concurrent map; publishing iterates the current listeners. Dead/failed listeners are dropped
 * so a broken SSE connection never blocks others (§8, §11.8).
 */
@Component
public class InProcessConversationEventBus implements ConversationEventBus {

    private static final Logger log = LoggerFactory.getLogger(InProcessConversationEventBus.class);

    // conversationId -> set of live listeners
    private final Map<UUID, Set<Listener>> listeners = new ConcurrentHashMap<>();

    @Override
    public Subscription subscribe(ConversationId conversationId, Listener listener) {
        UUID key = conversationId.value();
        listeners.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet()).add(listener);
        log.debug("SSE subscribe: conv={} listeners={}", key, listeners.get(key).size());
        return () -> {
            Set<Listener> set = listeners.get(key);
            if (set != null) {
                set.remove(listener);
                if (set.isEmpty()) listeners.remove(key, set); // prune empty buckets
            }
        };
    }

    @Override
    public void publish(ConversationId conversationId, String event, Map<String, Object> payload) {
        Set<Listener> set = listeners.get(conversationId.value());
        if (set == null || set.isEmpty()) return;
        for (Listener l : set) {
            try {
                l.onEvent(event, payload);
            } catch (RuntimeException e) {
                // a failed listener (e.g. closed emitter) shouldn't stop delivery to others
                log.debug("listener failed, dropping: {}", e.toString());
                set.remove(l);
            }
        }
    }
}
