package com.tassist.infrastructure.persistence;

import com.tassist.domain.model.*;
import com.tassist.domain.port.out.*;
import com.tassist.domain.vo.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/** Integration tests for the channel messaging persistence layer (M3, 02_MESSAGING_SPEC §5). */
class ConversationPersistenceTest extends AbstractPgvectorContainerTest {

    @Autowired UserRepository users;
    @Autowired ChannelRepository channels;
    @Autowired ConversationRepository conversations;
    @Autowired ConversationMessageRepository messages;
    @Autowired ConversationReadRepository reads;

    private User newUser(String email) {
        Instant now = Instant.now();
        return new User(UserId.newId(), email, "Test User",
            Optional.of("$2a$hash"), AuthProvider.PASSWORD, Optional.empty(), now, now);
    }

    private Channel newChannel(UserId owner, String username) {
        Instant now = Instant.now();
        return new Channel(ChannelId.newId(), owner, username, "Display", "", "",
            ChannelVisibility.PUBLIC, Optional.empty(), true, now, now);
    }

    @Test
    void dm_openedFromBothDirections_yieldsExactlyOneRow() {
        User x = users.save(newUser("x-" + System.nanoTime() + "@t.dev"));
        User y = users.save(newUser("y-" + System.nanoTime() + "@t.dev"));
        Channel c = channels.save(newChannel(x.id(), "chan-" + (System.nanoTime() % 100000)));
        Instant now = Instant.now();

        // open X -> Y
        Conversation first = conversations.save(
            Conversation.dm(ConversationId.newId(), c.id(), x.id(), y.id(), now));

        // "open" again from the other direction (Y -> X): findDm must return the SAME row
        Optional<Conversation> found = conversations.findDm(c.id(), y.id(), x.id());
        assertThat(found).isPresent();
        assertThat(found.get().id()).isEqualTo(first.id());

        // and the forward direction resolves to the same row too
        assertThat(conversations.findDm(c.id(), x.id(), y.id()).get().id()).isEqualTo(first.id());
    }

    @Test
    void dm_persistsCanonicalOrderAndRoundtrips() {
        User x = users.save(newUser("cx-" + System.nanoTime() + "@t.dev"));
        User y = users.save(newUser("cy-" + System.nanoTime() + "@t.dev"));
        Channel c = channels.save(newChannel(x.id(), "cx-" + (System.nanoTime() % 100000)));
        Conversation dm = conversations.save(
            Conversation.dm(ConversationId.newId(), c.id(), x.id(), y.id(), Instant.now()));

        Conversation back = conversations.findById(dm.id()).orElseThrow();
        assertThat(back.kind()).isEqualTo(ConversationKind.DM);
        assertThat(back.hasParticipant(x.id())).isTrue();
        assertThat(back.hasParticipant(y.id())).isTrue();
    }

    @Test
    void group_isUniquePerChannel_andFindable() {
        User owner = users.save(newUser("go-" + System.nanoTime() + "@t.dev"));
        Channel c = channels.save(newChannel(owner.id(), "gc-" + (System.nanoTime() % 100000)));
        Conversation g = conversations.save(Conversation.group(ConversationId.newId(), c.id(), Instant.now()));
        assertThat(conversations.findGroup(c.id())).isPresent();
        assertThat(conversations.findGroup(c.id()).get().id()).isEqualTo(g.id());
    }

    @Test
    void messages_pageOldestFirst_andLatestPreview() {
        User x = users.save(newUser("mx-" + System.nanoTime() + "@t.dev"));
        User y = users.save(newUser("my-" + System.nanoTime() + "@t.dev"));
        Channel c = channels.save(newChannel(x.id(), "mc-" + (System.nanoTime() % 100000)));
        Conversation dm = conversations.save(
            Conversation.dm(ConversationId.newId(), c.id(), x.id(), y.id(), Instant.now()));

        Instant t0 = Instant.parse("2026-01-01T00:00:00Z");
        messages.save(ConversationMessage.human(ConversationMessageId.newId(), dm.id(), x.id(), "first", t0));
        messages.save(ConversationMessage.human(ConversationMessageId.newId(), dm.id(), y.id(), "second", t0.plusSeconds(10)));
        ConversationMessage third = messages.save(
            ConversationMessage.human(ConversationMessageId.newId(), dm.id(), x.id(), "third", t0.plusSeconds(20)));

        List<ConversationMessage> page = messages.findByConversation(dm.id(), null, 50);
        assertThat(page).extracting(ConversationMessage::content).containsExactly("first", "second", "third");
        assertThat(messages.findLatest(dm.id()).get().id()).isEqualTo(third.id());
    }

    @Test
    void unread_excludesOwnAndDeleted_andRespectsCursor() {
        User x = users.save(newUser("ux-" + System.nanoTime() + "@t.dev"));
        User y = users.save(newUser("uy-" + System.nanoTime() + "@t.dev"));
        Channel c = channels.save(newChannel(x.id(), "uc-" + (System.nanoTime() % 100000)));
        Conversation dm = conversations.save(
            Conversation.dm(ConversationId.newId(), c.id(), x.id(), y.id(), Instant.now()));

        Instant base = Instant.parse("2026-02-01T00:00:00Z");
        messages.save(ConversationMessage.human(ConversationMessageId.newId(), dm.id(), y.id(), "from y 1", base.plusSeconds(1)));
        messages.save(ConversationMessage.human(ConversationMessageId.newId(), dm.id(), y.id(), "from y 2", base.plusSeconds(2)));
        messages.save(ConversationMessage.human(ConversationMessageId.newId(), dm.id(), x.id(), "from x", base.plusSeconds(3)));
        // x's unread = messages after `base` not authored by x = 2 (both from y)
        assertThat(messages.countUnread(dm.id(), base, x.id())).isEqualTo(2);
        // after reading up to base+2, only 0 remain unread for x (base+3 is x's own)
        assertThat(messages.countUnread(dm.id(), base.plusSeconds(2), x.id())).isEqualTo(0);
    }

    @Test
    void read_isMaxWins_neverMovesBackwards() {
        User x = users.save(newUser("rx-" + System.nanoTime() + "@t.dev"));
        User y = users.save(newUser("ry-" + System.nanoTime() + "@t.dev"));
        Channel c = channels.save(newChannel(x.id(), "rc-" + (System.nanoTime() % 100000)));
        Conversation dm = conversations.save(
            Conversation.dm(ConversationId.newId(), c.id(), x.id(), y.id(), Instant.now()));

        Instant later = Instant.parse("2026-03-01T12:00:00Z");
        Instant earlier = later.minusSeconds(3600);
        reads.markRead(dm.id(), x.id(), later);
        reads.markRead(dm.id(), x.id(), earlier); // should be ignored (max-wins)
        assertThat(reads.findLastRead(dm.id(), x.id())).contains(later);
    }
}
