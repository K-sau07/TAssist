package com.tassist.infrastructure.web.channel;

import com.tassist.domain.error.Unauthenticated;
import com.tassist.domain.error.ValidationError;
import com.tassist.domain.model.Membership;
import com.tassist.domain.port.in.MembershipUseCase;
import com.tassist.domain.port.out.UserRepository;
import com.tassist.domain.model.User;
import com.tassist.domain.vo.*;
import com.tassist.infrastructure.web.channel.ChannelDtos.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/** §12.5 owner membership actions + §12.6 visitor join/leave. */
@RestController
@RequestMapping("/api/channels/{channelId}")
public class MembershipController {

    private final MembershipUseCase memberships;
    private final UserRepository users;

    public MembershipController(MembershipUseCase memberships, UserRepository users) {
        this.memberships = memberships;
        this.users = users;
    }

    // ---- visitor side (§12.6) ----

    @PostMapping("/join")
    public MembershipView join(@PathVariable String channelId, @RequestBody(required = false) JoinRequest req,
                               Authentication auth) {
        UserId user = principal(auth);
        Optional<String> msg = req == null ? Optional.empty() : Optional.ofNullable(req.message());
        Membership m = memberships.requestJoin(user, channelId(channelId), msg);
        return MembershipView.of(m);
    }

    @DeleteMapping("/membership")
    public ResponseEntity<Void> leave(@PathVariable String channelId, Authentication auth) {
        memberships.leave(principal(auth), channelId(channelId));
        return ResponseEntity.noContent().build();
    }

    // ---- owner side (§12.5) ----

    @GetMapping("/members")
    public List<MembershipView> members(@PathVariable String channelId,
                                        @RequestParam(value = "status", defaultValue = "PENDING") String status,
                                        Authentication auth) {
        UserId owner = principal(auth);
        return memberships.listByStatus(owner, channelId(channelId), ChannelDtos.parseStatus(status))
            .stream().map(m -> {
                Optional<User> u = users.findById(m.userId());
                return MembershipView.of(m,
                    u.map(User::displayName).orElse("Unknown user"),
                    u.map(User::email).orElse(null));
            }).toList();
    }

    @PostMapping("/members/{membershipId}/approve")
    public MembershipView approve(@PathVariable String channelId, @PathVariable String membershipId,
                                  Authentication auth) {
        return MembershipView.of(memberships.approve(principal(auth), channelId(channelId), membershipId(membershipId)));
    }

    @PostMapping("/members/{membershipId}/deny")
    public MembershipView deny(@PathVariable String channelId, @PathVariable String membershipId,
                               Authentication auth) {
        return MembershipView.of(memberships.deny(principal(auth), channelId(channelId), membershipId(membershipId)));
    }

    @PostMapping("/members/{membershipId}/kick")
    public MembershipView kick(@PathVariable String channelId, @PathVariable String membershipId,
                               Authentication auth) {
        return MembershipView.of(memberships.kick(principal(auth), channelId(channelId), membershipId(membershipId)));
    }

    @PostMapping("/members/{membershipId}/ban")
    public MembershipView ban(@PathVariable String channelId, @PathVariable String membershipId,
                              Authentication auth) {
        return MembershipView.of(memberships.ban(principal(auth), channelId(channelId), membershipId(membershipId)));
    }

    @PostMapping("/members/{membershipId}/reinvite")
    public MembershipView reinvite(@PathVariable String channelId, @PathVariable String membershipId,
                                   @RequestBody(required = false) JoinRequest req, Authentication auth) {
        String note = req == null ? "" : (req.message() == null ? "" : req.message());
        return MembershipView.of(memberships.reinvite(principal(auth), channelId(channelId),
            membershipId(membershipId), note));
    }

    // ---- helpers ----
    private UserId principal(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof UserId userId))
            throw new Unauthenticated("authentication required");
        return userId;
    }
    private ChannelId channelId(String raw) {
        try { return ChannelId.of(raw); } catch (Exception e) { throw new ValidationError("invalid channelId"); }
    }
    private MembershipId membershipId(String raw) {
        try { return MembershipId.of(raw); } catch (Exception e) { throw new ValidationError("invalid membershipId"); }
    }
}
