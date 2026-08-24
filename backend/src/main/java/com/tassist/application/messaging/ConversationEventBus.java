package com.tassist.application.messaging;

import com.tassist.domain.vo.ConversationId;

import java.util.Map;

/**
 * Fan-out bus for realtime conversation events (02_MESSAGING_SPEC §8). Publishers (the service
 * layer) emit events for a conversation; subscribers (SSE connections) receive them.
 *
 * <p>v1 is a single-instance in-process implementation. The interface exists so a multi-instance
 * deployment can swap in a Redis pub/sub backing (Redis is already a dependency) without touching
 * callers.
 */
public interface ConversationEventBus {

    /** A live subscription; call {@link #close()} to unsubscribe. */
    interface Subscription extends AutoCloseable {
        @Override void close();
    }

    /** Register a listener for a conversation's events. The listener is (eventName, payload). */
    Subscription subscribe(ConversationId conversationId, Listener listener);

    /** Publish an event to all current subscribers of a conversation. Never throws. */
    void publish(ConversationId conversationId, String event, Map<String, Object> payload);

    @FunctionalInterface
    interface Listener {
        void onEvent(String event, Map<String, Object> payload);
    }
}
