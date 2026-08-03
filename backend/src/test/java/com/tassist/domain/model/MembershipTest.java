package com.tassist.domain.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tassist.domain.vo.ChannelId;
import com.tassist.domain.vo.MembershipId;
import com.tassist.domain.vo.MembershipStatus;
import com.tassist.domain.vo.UserId;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MembershipTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private static Membership at(MembershipStatus status) {
        return new Membership(MembershipId.newId(), ChannelId.newId(), UserId.newId(), status,
                Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), NOW, NOW);
    }

    @Test
    void pending_allowsApproveRejectBan() {
        Membership m = at(MembershipStatus.PENDING);
        assertTrue(m.canTransitionTo(MembershipStatus.APPROVED));
        assertTrue(m.canTransitionTo(MembershipStatus.REJECTED));
        assertTrue(m.canTransitionTo(MembershipStatus.BANNED));
    }

    @Test
    void pending_disallowsLeft() {
        assertFalse(at(MembershipStatus.PENDING).canTransitionTo(MembershipStatus.LEFT));
    }

    @Test
    void approved_allowsLeftAndBanOnly() {
        Membership m = at(MembershipStatus.APPROVED);
        assertTrue(m.canTransitionTo(MembershipStatus.LEFT));
        assertTrue(m.canTransitionTo(MembershipStatus.BANNED));
        assertFalse(m.canTransitionTo(MembershipStatus.APPROVED));
        assertFalse(m.canTransitionTo(MembershipStatus.REJECTED));
    }

    @Test
    void rejected_allowsPendingOnly() {
        Membership m = at(MembershipStatus.REJECTED);
        assertTrue(m.canTransitionTo(MembershipStatus.PENDING));
        assertFalse(m.canTransitionTo(MembershipStatus.APPROVED));
        assertFalse(m.canTransitionTo(MembershipStatus.BANNED));
    }

    @Test
    void left_allowsPendingOnly() {
        Membership m = at(MembershipStatus.LEFT);
        assertTrue(m.canTransitionTo(MembershipStatus.PENDING));
        assertFalse(m.canTransitionTo(MembershipStatus.APPROVED));
    }

    @Test
    void banned_isTerminal() {
        Membership m = at(MembershipStatus.BANNED);
        for (MembershipStatus s : MembershipStatus.values()) {
            assertFalse(m.canTransitionTo(s), "BANNED must not transition to " + s);
        }
    }
}
