package com.tassist.application.messaging;

import com.tassist.domain.error.Forbidden;
import com.tassist.domain.error.NotFoundError;
import com.tassist.domain.error.ValidationError;
import com.tassist.domain.model.*;
import com.tassist.domain.port.out.*;
import com.tassist.domain.vo.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

/** Unit tests for ConversationService access matrix (§6) + edge cases (§11), fakes only. */
class ConversationServiceTest {

    // ── fakes ──
    static class FakeConversations implements ConversationRepository {
        final Map<UUID,Conversation> byId = new HashMap<>();
        public Conversation save(Conversation c){ byId.put(c.id().value(), c); return c; }
        public Optional<Conversation> findById(ConversationId id){ return Optional.ofNullable(byId.get(id.value())); }
        public Optional<Conversation> findDm(ChannelId ch, UserId x, UserId y){
            return byId.values().stream().filter(c -> c.kind()==ConversationKind.DM
                && c.channelId().equals(ch) && c.hasParticipant(x) && c.hasParticipant(y)).findFirst();
        }
        public Optional<Conversation> findGroup(ChannelId ch){
            return byId.values().stream().filter(c -> c.kind()==ConversationKind.GROUP && c.channelId().equals(ch)).findFirst();
        }
        public List<Conversation> findDmsForUser(ChannelId ch, UserId u){
            return byId.values().stream().filter(c -> c.kind()==ConversationKind.DM
                && c.channelId().equals(ch) && c.hasParticipant(u)).toList();
        }
        public void delete(ConversationId id){ byId.remove(id.value()); }
    }
    static class FakeMessages implements ConversationMessageRepository {
        final Map<UUID,ConversationMessage> byId = new LinkedHashMap<>();
        public ConversationMessage save(ConversationMessage m){ byId.put(m.id().value(), m); return m; }
        public Optional<ConversationMessage> findById(ConversationMessageId id){ return Optional.ofNullable(byId.get(id.value())); }
        public List<ConversationMessage> findByConversation(ConversationId c, Instant before, int limit){
            return byId.values().stream().filter(m -> m.conversationId().equals(c))
                .filter(m -> before == null || m.createdAt().isBefore(before))
                .sorted(Comparator.comparing(ConversationMessage::createdAt)).limit(limit).toList();
        }
        public Optional<ConversationMessage> findLatest(ConversationId c){
            return byId.values().stream().filter(m -> m.conversationId().equals(c) && !m.isDeleted())
                .max(Comparator.comparing(ConversationMessage::createdAt));
        }
        public long countUnread(ConversationId c, Instant after, UserId exclude){
            return byId.values().stream().filter(m -> m.conversationId().equals(c)
                && m.createdAt().isAfter(after) && !m.isDeleted()
                && m.senderId().map(s -> !s.equals(exclude)).orElse(true)).count();
        }
    }
    static class FakeReads implements ConversationReadRepository {
        final Map<String,Instant> map = new HashMap<>();
        String k(ConversationId c, UserId u){ return c.value()+"|"+u.value(); }
        public Optional<Instant> findLastRead(ConversationId c, UserId u){ return Optional.ofNullable(map.get(k(c,u))); }
        public void markRead(ConversationId c, UserId u, Instant at){
            Instant cur = map.get(k(c,u));
            if (cur == null || at.isAfter(cur)) map.put(k(c,u), at);
        }
    }
    static class FakeChannels implements ChannelRepository {
        final Map<UUID,Channel> byId = new HashMap<>();
        public Channel save(Channel c){ byId.put(c.id().value(), c); return c; }
        public Optional<Channel> findById(ChannelId id){ return Optional.ofNullable(byId.get(id.value())); }
        public Optional<Channel> findByUsername(String u){ return Optional.empty(); }
        public List<Channel> findByOwner(UserId o){ return List.of(); }
        public boolean existsByUsername(String u){ return false; }
        public List<Channel> searchByUsernameOrDisplayName(String q,int l){ return List.of(); }
        public List<Channel> findPublic(int p,int s){ return List.of(); }
        public List<Channel> findJoined(UserId u){ return List.of(); }
        public void delete(ChannelId id){}
    }
    static class FakeMemberships implements MembershipRepository {
        final Map<UUID,Membership> byId = new HashMap<>();
        public Membership save(Membership m){ byId.put(m.id().value(),m); return m; }
        public Optional<Membership> findById(MembershipId id){ return Optional.ofNullable(byId.get(id.value())); }
        public Optional<Membership> findByChannelAndUser(ChannelId c, UserId u){
            return byId.values().stream().filter(m->m.channelId().equals(c)&&m.userId().equals(u)).findFirst(); }
        public List<Membership> findByChannelAndStatus(ChannelId c, MembershipStatus s){
            return byId.values().stream().filter(m->m.channelId().equals(c)&&m.status()==s).toList(); }
        public List<Membership> findByUserAndStatus(UserId u, MembershipStatus s){
            return byId.values().stream().filter(m->m.userId().equals(u)&&m.status()==s).toList(); }
        public List<Membership> findByChannel(ChannelId c){
            return byId.values().stream().filter(m->m.channelId().equals(c)).toList(); }
    }
    static class FakeUsers implements UserRepository {
        final Map<UUID,User> byId = new HashMap<>();
        public User save(User u){ byId.put(u.id().value(),u); return u; }
        public Optional<User> findById(UserId id){ return Optional.ofNullable(byId.get(id.value())); }
        public Optional<User> findByEmail(String e){ return Optional.empty(); }
        public Optional<User> findByGoogleSubject(String s){ return Optional.empty(); }
        public boolean existsByEmail(String e){ return false; }
    }

    FakeConversations conversations; FakeMessages messages; FakeReads reads;
    FakeChannels channels; FakeMemberships memberships; FakeUsers users;
    ConversationService svc;
    UserId owner, memberA, memberB, outsider;
    Channel channel;

    private User user(UserId id, String email){
        Instant now = Instant.now();
        User u = new User(id, email, "Name "+email, Optional.of("$h"), AuthProvider.PASSWORD, Optional.empty(), now, now);
        users.save(u); return u;
    }
    private void approve(UserId u){
        Instant now = Instant.now();
        memberships.save(new Membership(MembershipId.newId(), channel.id(), u, MembershipStatus.APPROVED,
            Optional.empty(), Optional.of(now), Optional.empty(), Optional.empty(), Optional.empty(), now, now));
    }

    @BeforeEach void setup(){
        conversations = new FakeConversations(); messages = new FakeMessages(); reads = new FakeReads();
        channels = new FakeChannels(); memberships = new FakeMemberships(); users = new FakeUsers();
        ConversationEventBus events = new ConversationEventBus() {
            public Subscription subscribe(com.tassist.domain.vo.ConversationId c, Listener l){ return () -> {}; }
            public void publish(com.tassist.domain.vo.ConversationId c, String e, java.util.Map<String,Object> p){}
        };
        svc = new ConversationService(conversations, messages, reads, channels, memberships, users, events);
        owner = UserId.newId(); memberA = UserId.newId(); memberB = UserId.newId(); outsider = UserId.newId();
        Instant now = Instant.now();
        channel = channels.save(new Channel(ChannelId.newId(), owner, "chan-x", "Chan", "", "",
            ChannelVisibility.PUBLIC, Optional.empty(), true, true, now, now));
        user(owner,"owner@t.dev"); user(memberA,"a@t.dev"); user(memberB,"b@t.dev"); user(outsider,"out@t.dev");
        approve(memberA); approve(memberB);
    }

    // ── DM open ──
    @Test void memberCanDmAnotherMember_idempotent(){
        Conversation c1 = svc.openOrCreateDm(memberA, channel.id(), memberB);
        Conversation c2 = svc.openOrCreateDm(memberB, channel.id(), memberA); // reverse direction
        assertThat(c2.id()).isEqualTo(c1.id()); // same thread
    }
    @Test void memberCanDmOwner(){
        assertThatCode(() -> svc.openOrCreateDm(memberA, channel.id(), owner)).doesNotThrowAnyException();
    }
    @Test void cannotDmYourself(){
        assertThatThrownBy(() -> svc.openOrCreateDm(memberA, channel.id(), memberA))
            .isInstanceOf(ValidationError.class);
    }
    @Test void outsiderCannotOpenDm(){
        assertThatThrownBy(() -> svc.openOrCreateDm(outsider, channel.id(), memberA))
            .isInstanceOf(Forbidden.class);
    }
    @Test void cannotDmAnOutsider(){
        assertThatThrownBy(() -> svc.openOrCreateDm(memberA, channel.id(), outsider))
            .isInstanceOf(ValidationError.class);
    }

    // ── posting + access ──
    @Test void participantCanPost_outsiderCannotRead(){
        Conversation dm = svc.openOrCreateDm(memberA, channel.id(), memberB);
        svc.postHuman(memberA, channel.id(), dm.id(), "hi");
        assertThatThrownBy(() -> svc.listMessages(outsider, channel.id(), dm.id(), null, 50))
            .isInstanceOf(Forbidden.class);
        // a third member who isn't in this DM also cannot read
        UserId memberC = UserId.newId(); user(memberC,"c@t.dev"); approve(memberC);
        assertThatThrownBy(() -> svc.listMessages(memberC, channel.id(), dm.id(), null, 50))
            .isInstanceOf(Forbidden.class);
    }
    @Test void emptyMessageRejected(){
        Conversation dm = svc.openOrCreateDm(memberA, channel.id(), memberB);
        assertThatThrownBy(() -> svc.postHuman(memberA, channel.id(), dm.id(), "   "))
            .isInstanceOf(ValidationError.class);
    }
    @Test void overlongMessageRejected(){
        Conversation dm = svc.openOrCreateDm(memberA, channel.id(), memberB);
        String big = "x".repeat(ConversationService.MAX_MESSAGE_CHARS + 1);
        assertThatThrownBy(() -> svc.postHuman(memberA, channel.id(), dm.id(), big))
            .isInstanceOf(ValidationError.class);
    }
    @Test void bannedMemberLosesAccess(){
        Conversation dm = svc.openOrCreateDm(memberA, channel.id(), memberB);
        // ban memberA
        Membership m = memberships.findByChannelAndUser(channel.id(), memberA).get();
        Instant now = Instant.now();
        memberships.save(new Membership(m.id(), channel.id(), memberA, MembershipStatus.BANNED,
            Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(now), Optional.empty(), now, now));
        assertThatThrownBy(() -> svc.postHuman(memberA, channel.id(), dm.id(), "still here?"))
            .isInstanceOf(Forbidden.class);
        // the other side can still read history
        assertThatCode(() -> svc.listMessages(memberB, channel.id(), dm.id(), null, 50)).doesNotThrowAnyException();
    }

    // ── delete ──
    @Test void senderCanDeleteOwn_othersCannot(){
        Conversation dm = svc.openOrCreateDm(memberA, channel.id(), memberB);
        var msg = svc.postHuman(memberA, channel.id(), dm.id(), "mine");
        assertThatThrownBy(() -> svc.deleteMessage(memberB, channel.id(), dm.id(), msg.id()))
            .isInstanceOf(Forbidden.class);
        assertThatCode(() -> svc.deleteMessage(memberA, channel.id(), dm.id(), msg.id())).doesNotThrowAnyException();
        assertThat(messages.findById(msg.id()).get().isDeleted()).isTrue();
    }
    @Test void ownerCanModerateGroupMessages(){
        Conversation grp = svc.openGroup(memberA, channel.id());
        var msg = svc.postHuman(memberA, channel.id(), grp.id(), "spam");
        // owner (not the sender) can delete in the group room
        assertThatCode(() -> svc.deleteMessage(owner, channel.id(), grp.id(), msg.id())).doesNotThrowAnyException();
        assertThat(messages.findById(msg.id()).get().isDeleted()).isTrue();
    }
    @Test void ownerCannotDeleteOthersDmMessages(){
        Conversation dm = svc.openOrCreateDm(memberA, channel.id(), memberB);
        var msg = svc.postHuman(memberA, channel.id(), dm.id(), "private");
        // owner isn't even a participant of this DM → no access at all
        assertThatThrownBy(() -> svc.deleteMessage(owner, channel.id(), dm.id(), msg.id()))
            .isInstanceOf(Forbidden.class);
    }

    // ── group toggle ──
    @Test void groupDisabled_blocksAccess(){
        channels.save(channel.withGroupChatEnabled(false));
        assertThatThrownBy(() -> svc.openGroup(memberA, channel.id()))
            .isInstanceOf(Forbidden.class);
    }

    // ── read + unread ──
    @Test void unreadCountsOthersMessages_notOwn(){
        Conversation dm = svc.openOrCreateDm(memberA, channel.id(), memberB);
        svc.postHuman(memberA, channel.id(), dm.id(), "1");
        svc.postHuman(memberA, channel.id(), dm.id(), "2");
        // memberB hasn't read → 2 unread; memberA's own → 0
        assertThat(svc.unreadCount(memberB, dm.id())).isEqualTo(2);
        assertThat(svc.unreadCount(memberA, dm.id())).isEqualTo(0);
        // after B reads, unread clears
        svc.markRead(memberB, channel.id(), dm.id(), Instant.now().plusSeconds(1));
        assertThat(svc.unreadCount(memberB, dm.id())).isEqualTo(0);
    }

    // ── participants list ──
    @Test void listParticipants_excludesSelf_includesOwnerAndMembers(){
        List<User> forA = svc.listParticipants(memberA, channel.id());
        assertThat(forA).extracting(u -> u.id()).contains(owner, memberB).doesNotContain(memberA, outsider);
    }
}
