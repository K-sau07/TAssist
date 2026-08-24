package com.tassist.domain.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tassist.domain.vo.ChunkId;
import com.tassist.domain.vo.ConversationId;
import com.tassist.domain.vo.ConversationMessageId;
import com.tassist.domain.vo.FileId;
import com.tassist.domain.vo.MessageSenderKind;
import com.tassist.domain.vo.UserId;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ConversationMessageTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final ConversationId CONV = ConversationId.newId();

    private static Citation cite() {
        return new Citation(FileId.newId(), ChunkId.newId(), "Lecture 3", Optional.of("snippet"));
    }

    @Test void human_requiresSender_andNoCitations() {
        assertDoesNotThrow(() -> ConversationMessage.human(
            ConversationMessageId.newId(), CONV, UserId.newId(), "hi", NOW));
    }

    @Test void human_rejectsMissingSender() {
        assertThrows(IllegalArgumentException.class, () -> new ConversationMessage(
            ConversationMessageId.newId(), CONV, MessageSenderKind.HUMAN, Optional.empty(),
            "hi", List.of(), NOW, Optional.empty()));
    }

    @Test void human_rejectsCitations() {
        assertThrows(IllegalArgumentException.class, () -> new ConversationMessage(
            ConversationMessageId.newId(), CONV, MessageSenderKind.HUMAN, Optional.of(UserId.newId()),
            "hi", List.of(cite()), NOW, Optional.empty()));
    }

    @Test void ai_hasNoSender_mayCarryCitations() {
        assertDoesNotThrow(() -> ConversationMessage.ai(
            ConversationMessageId.newId(), CONV, "grounded answer", List.of(cite()), NOW));
    }

    @Test void ai_rejectsSender() {
        assertThrows(IllegalArgumentException.class, () -> new ConversationMessage(
            ConversationMessageId.newId(), CONV, MessageSenderKind.AI, Optional.of(UserId.newId()),
            "answer", List.of(), NOW, Optional.empty()));
    }

    @Test void softDelete_setsTombstone_andIsIdempotent() {
        ConversationMessage m = ConversationMessage.human(
            ConversationMessageId.newId(), CONV, UserId.newId(), "bye", NOW);
        assertFalse(m.isDeleted());
        ConversationMessage d = m.deleted(NOW.plusSeconds(10));
        assertTrue(d.isDeleted());
        assertEquals(NOW.plusSeconds(10), d.deletedAt().orElseThrow());
        // idempotent: deleting again returns the same instance, keeps original tombstone time
        assertSame(d, d.deleted(NOW.plusSeconds(999)));
    }

    @Test void softDelete_preservesContentAndId() {
        ConversationMessage m = ConversationMessage.human(
            ConversationMessageId.newId(), CONV, UserId.newId(), "keep me", NOW);
        ConversationMessage d = m.deleted(NOW.plusSeconds(1));
        assertEquals(m.id(), d.id());
        assertEquals("keep me", d.content());
    }
}
