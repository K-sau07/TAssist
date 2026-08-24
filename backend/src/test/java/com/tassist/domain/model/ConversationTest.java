package com.tassist.domain.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tassist.domain.vo.ChannelId;
import com.tassist.domain.vo.ConversationId;
import com.tassist.domain.vo.ConversationKind;
import com.tassist.domain.vo.UserId;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConversationTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    // two users with known ordering: LOW.value() < HIGH.value()
    private static final UserId LOW  = UserId.of(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    private static final UserId HIGH = UserId.of(UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"));

    @Test void dmFactory_normalisesToCanonicalOrder_regardlessOfArgOrder() {
        Conversation c1 = Conversation.dm(ConversationId.newId(), ChannelId.newId(), HIGH, LOW, NOW);
        Conversation c2 = Conversation.dm(ConversationId.newId(), ChannelId.newId(), LOW, HIGH, NOW);
        // both must place LOW as participantA
        assertEquals(LOW, c1.participantA().orElseThrow());
        assertEquals(HIGH, c1.participantB().orElseThrow());
        assertEquals(LOW, c2.participantA().orElseThrow());
        assertEquals(HIGH, c2.participantB().orElseThrow());
    }

    @Test void dmConstructor_rejectsNonCanonicalOrder() {
        // building directly with HIGH first must fail — forces callers through dm(...)
        assertThrows(IllegalArgumentException.class, () -> new Conversation(
            ConversationId.newId(), ChannelId.newId(), ConversationKind.DM,
            Optional.of(HIGH), Optional.of(LOW), NOW, NOW));
    }

    @Test void dm_rejectsIdenticalParticipants() {
        assertThrows(IllegalArgumentException.class,
            () -> Conversation.dm(ConversationId.newId(), ChannelId.newId(), LOW, LOW, NOW));
    }

    @Test void dm_rejectsMissingParticipant() {
        assertThrows(IllegalArgumentException.class, () -> new Conversation(
            ConversationId.newId(), ChannelId.newId(), ConversationKind.DM,
            Optional.of(LOW), Optional.empty(), NOW, NOW));
    }

    @Test void group_hasNoParticipants() {
        assertDoesNotThrow(() -> Conversation.group(ConversationId.newId(), ChannelId.newId(), NOW));
    }

    @Test void group_rejectsParticipants() {
        assertThrows(IllegalArgumentException.class, () -> new Conversation(
            ConversationId.newId(), ChannelId.newId(), ConversationKind.GROUP,
            Optional.of(LOW), Optional.empty(), NOW, NOW));
    }

    @Test void hasParticipant_trueForBothParties_falseForOutsider() {
        Conversation dm = Conversation.dm(ConversationId.newId(), ChannelId.newId(), LOW, HIGH, NOW);
        assertTrue(dm.hasParticipant(LOW));
        assertTrue(dm.hasParticipant(HIGH));
        assertFalse(dm.hasParticipant(UserId.newId()));
    }

    @Test void otherParticipant_returnsTheOppositeParty() {
        Conversation dm = Conversation.dm(ConversationId.newId(), ChannelId.newId(), LOW, HIGH, NOW);
        assertEquals(HIGH, dm.otherParticipant(LOW).orElseThrow());
        assertEquals(LOW, dm.otherParticipant(HIGH).orElseThrow());
        assertTrue(dm.otherParticipant(UserId.newId()).isEmpty());
    }

    @Test void group_hasParticipant_alwaysFalse() {
        Conversation g = Conversation.group(ConversationId.newId(), ChannelId.newId(), NOW);
        assertFalse(g.hasParticipant(LOW));
        assertTrue(g.otherParticipant(LOW).isEmpty());
    }

    @Test void touched_bumpsUpdatedAtOnly() {
        Conversation dm = Conversation.dm(ConversationId.newId(), ChannelId.newId(), LOW, HIGH, NOW);
        Instant later = NOW.plusSeconds(60);
        Conversation t = dm.touched(later);
        assertEquals(later, t.updatedAt());
        assertEquals(NOW, t.createdAt());
        assertEquals(dm.id(), t.id());
    }
}
