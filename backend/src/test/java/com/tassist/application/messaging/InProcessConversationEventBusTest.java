package com.tassist.application.messaging;

import com.tassist.application.messaging.ConversationEventBus.Subscription;
import com.tassist.domain.vo.ConversationId;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/** Unit tests for the in-process fan-out bus (§8). */
class InProcessConversationEventBusTest {

    @Test void deliversToAllSubscribers() {
        var bus = new InProcessConversationEventBus();
        ConversationId conv = ConversationId.newId();
        List<String> a = new ArrayList<>(), b = new ArrayList<>();
        bus.subscribe(conv, (e, p) -> a.add(e));
        bus.subscribe(conv, (e, p) -> b.add(e));
        bus.publish(conv, "message", Map.of("x", 1));
        assertThat(a).containsExactly("message");
        assertThat(b).containsExactly("message");
    }

    @Test void unsubscribe_stopsDelivery() {
        var bus = new InProcessConversationEventBus();
        ConversationId conv = ConversationId.newId();
        AtomicInteger count = new AtomicInteger();
        Subscription sub = bus.subscribe(conv, (e, p) -> count.incrementAndGet());
        bus.publish(conv, "message", Map.of());
        sub.close();
        bus.publish(conv, "message", Map.of());
        assertThat(count.get()).isEqualTo(1);
    }

    @Test void publishToUnknownConversation_isNoop() {
        var bus = new InProcessConversationEventBus();
        // must not throw when nobody's listening
        bus.publish(ConversationId.newId(), "message", Map.of());
    }

    @Test void isolatesConversations() {
        var bus = new InProcessConversationEventBus();
        ConversationId c1 = ConversationId.newId(), c2 = ConversationId.newId();
        List<String> got = new ArrayList<>();
        bus.subscribe(c1, (e, p) -> got.add(e));
        bus.publish(c2, "message", Map.of()); // different conversation
        assertThat(got).isEmpty();
    }

    @Test void failingListener_isDropped_othersStillReceive() {
        var bus = new InProcessConversationEventBus();
        ConversationId conv = ConversationId.newId();
        List<String> good = new ArrayList<>();
        bus.subscribe(conv, (e, p) -> { throw new RuntimeException("boom"); });
        bus.subscribe(conv, (e, p) -> good.add(e));
        bus.publish(conv, "message", Map.of());
        assertThat(good).containsExactly("message");
        // the failing listener was pruned → second publish still reaches the good one
        bus.publish(conv, "message", Map.of());
        assertThat(good).hasSize(2);
    }
}
