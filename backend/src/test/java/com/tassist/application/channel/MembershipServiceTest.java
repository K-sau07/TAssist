package com.tassist.application.channel;

import com.tassist.domain.error.*;
import com.tassist.domain.model.Channel;
import com.tassist.domain.model.Membership;
import com.tassist.domain.port.out.ChannelRepository;
import com.tassist.domain.port.out.MembershipRepository;
import com.tassist.domain.vo.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

/** Unit tests for the Membership state machine + re-request rules (§8/§12.6), fakes only. */
class MembershipServiceTest {

    static class FakeMemberships implements MembershipRepository {
        final Map<UUID,Membership> byId = new HashMap<>();
        public Membership save(Membership m){ byId.put(m.id().value(),m); return m; }
        public Optional<Membership> findById(MembershipId id){ return Optional.ofNullable(byId.get(id.value())); }
        public Optional<Membership> findByChannelAndUser(ChannelId c, UserId u){
            return byId.values().stream().filter(m->m.channelId().equals(c)&&m.userId().equals(u)).findFirst();
        }
        public List<Membership> findByChannelAndStatus(ChannelId c, MembershipStatus s){
            return byId.values().stream().filter(m->m.channelId().equals(c)&&m.status()==s).toList();
        }
        public List<Membership> findByUserAndStatus(UserId u, MembershipStatus s){
            return byId.values().stream().filter(m->m.userId().equals(u)&&m.status()==s).toList();
        }
        public List<Membership> findByChannel(ChannelId c){
            return byId.values().stream().filter(m->m.channelId().equals(c)).toList();
        }
    }
    static class FakeChannels implements ChannelRepository {
        final Map<UUID,Channel> byId = new HashMap<>();
        public Channel save(Channel c){ byId.put(c.id().value(),c); return c; }
        public Optional<Channel> findById(ChannelId id){ return Optional.ofNullable(byId.get(id.value())); }
        public Optional<Channel> findByUsername(String u){ return Optional.empty(); }
        public List<Channel> findByOwner(UserId o){ return List.of(); }
        public boolean existsByUsername(String u){ return false; }
        public List<Channel> searchByUsernameOrDisplayName(String q,int l){ return List.of(); }
        public List<Channel> findPublic(int p,int s){ return List.of(); }
        public void delete(ChannelId id){}
    }

    FakeMemberships memberships;
    FakeChannels channels;
    MembershipService svc;
    UserId owner, visitor;
    ChannelId channelId;

    @BeforeEach void setup(){
        memberships = new FakeMemberships();
        channels = new FakeChannels();
        svc = new MembershipService(memberships, channels);
        owner = UserId.newId(); visitor = UserId.newId();
        channelId = ChannelId.newId();
        channels.save(makeChannel(channelId, owner, true)); // requireMessageOnReRequest=true
    }
    private Channel makeChannel(ChannelId id, UserId o, boolean reqMsg){
        Instant now=Instant.now();
        return new Channel(id, o, "chan-x", "Chan X", "", "", ChannelVisibility.PUBLIC,
            Optional.empty(), reqMsg, now, now);
    }

    @Test void request_join_creates_pending(){
        Membership m = svc.requestJoin(visitor, channelId, Optional.of("hi"));
        assertThat(m.status()).isEqualTo(MembershipStatus.PENDING);
        assertThat(m.requestMessage()).contains("hi");
    }

    @Test void owner_cannot_join_own_channel(){
        assertThatThrownBy(()->svc.requestJoin(owner, channelId, Optional.empty()))
            .isInstanceOf(ValidationError.class);
    }

    @Test void duplicate_pending_request_conflicts(){
        svc.requestJoin(visitor, channelId, Optional.of("hi"));
        assertThatThrownBy(()->svc.requestJoin(visitor, channelId, Optional.of("again")))
            .isInstanceOf(ConflictError.class);
    }

    @Test void approve_moves_pending_to_approved(){
        Membership m = svc.requestJoin(visitor, channelId, Optional.of("hi"));
        Membership a = svc.approve(owner, channelId, m.id());
        assertThat(a.status()).isEqualTo(MembershipStatus.APPROVED);
        assertThat(a.approvedAt()).isPresent();
    }

    @Test void deny_then_rerequest_requires_message_when_channel_demands_it(){
        Membership m = svc.requestJoin(visitor, channelId, Optional.of("hi"));
        svc.deny(owner, channelId, m.id());
        // re-request without a message → rejected (channel requires it)
        assertThatThrownBy(()->svc.requestJoin(visitor, channelId, Optional.empty()))
            .isInstanceOf(ValidationError.class);
        // with a message → back to PENDING
        Membership re = svc.requestJoin(visitor, channelId, Optional.of("please reconsider"));
        assertThat(re.status()).isEqualTo(MembershipStatus.PENDING);
    }

    @Test void non_owner_cannot_approve(){
        Membership m = svc.requestJoin(visitor, channelId, Optional.of("hi"));
        UserId stranger = UserId.newId();
        assertThatThrownBy(()->svc.approve(stranger, channelId, m.id()))
            .isInstanceOf(Forbidden.class);
    }

    @Test void ban_is_terminal_then_reinvite_reopens(){
        Membership m = svc.requestJoin(visitor, channelId, Optional.of("hi"));
        Membership banned = svc.ban(owner, channelId, m.id());
        assertThat(banned.status()).isEqualTo(MembershipStatus.BANNED);
        // banned user cannot re-request
        assertThatThrownBy(()->svc.requestJoin(visitor, channelId, Optional.of("let me back")))
            .isInstanceOf(Forbidden.class);
        // owner reinvite → PENDING
        Membership re = svc.reinvite(owner, channelId, m.id(), "second chance");
        assertThat(re.status()).isEqualTo(MembershipStatus.PENDING);
    }

    @Test void approve_from_approved_is_illegal_transition(){
        Membership m = svc.requestJoin(visitor, channelId, Optional.of("hi"));
        svc.approve(owner, channelId, m.id());
        assertThatThrownBy(()->svc.approve(owner, channelId, m.id()))
            .isInstanceOf(ConflictError.class);
    }

    @Test void leave_after_approved(){
        Membership m = svc.requestJoin(visitor, channelId, Optional.of("hi"));
        svc.approve(owner, channelId, m.id());
        svc.leave(visitor, channelId);
        assertThat(memberships.findByChannelAndUser(channelId, visitor).orElseThrow().status())
            .isEqualTo(MembershipStatus.LEFT);
    }
}
