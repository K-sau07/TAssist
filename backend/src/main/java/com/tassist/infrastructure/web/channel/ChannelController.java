package com.tassist.infrastructure.web.channel;

import com.tassist.domain.error.Unauthenticated;
import com.tassist.domain.error.ValidationError;
import com.tassist.domain.model.Channel;
import com.tassist.domain.port.in.ChannelUseCase;
import com.tassist.domain.port.in.ChannelUseCase.CreateChannelCommand;
import com.tassist.domain.port.in.ChannelUseCase.EditChannelCommand;
import com.tassist.domain.port.in.MembershipUseCase;
import com.tassist.domain.vo.*;
import com.tassist.infrastructure.web.channel.ChannelDtos.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Optional;

/** §12.5 channel + channel-file endpoints (owner side) and the public @{username} view. */
@RestController
@RequestMapping("/api/channels")
public class ChannelController {

    private final ChannelUseCase channels;
    private final MembershipUseCase memberships;

    public ChannelController(ChannelUseCase channels, MembershipUseCase memberships) {
        this.channels = channels;
        this.memberships = memberships;
    }

    @PostMapping
    public ResponseEntity<ChannelView> create(@RequestBody CreateChannelRequest req, Authentication auth) {
        UserId user = principal(auth);
        if (req == null || req.username() == null || req.displayName() == null)
            throw new ValidationError("username and displayName are required");
        Channel c = channels.create(user, new CreateChannelCommand(
            req.username(), req.displayName(),
            req.description() == null ? "" : req.description(),
            req.expectationSummary() == null ? "" : req.expectationSummary(),
            ChannelDtos.parseVisibility(req.visibility()),
            req.requireMessageOnReRequest() != null && req.requireMessageOnReRequest()));
        return ResponseEntity.created(URI.create("/api/channels/@" + c.username())).body(ChannelView.of(c));
    }

    @GetMapping("/mine")
    public List<ChannelView> mine(Authentication auth) {
        return channels.listOwned(principal(auth)).stream().map(ChannelView::of).toList();
    }

    @GetMapping("/@{username}")
    public ChannelPublicView publicView(@PathVariable String username, Authentication auth) {
        UserId user = principal(auth);
        Channel c = channels.getByUsername(username);
        String myStatus = c.ownerId().equals(user) ? "OWNER"
            : memberships.myMembership(user, c.id()).map(m -> m.status().name()).orElse(null);
        return new ChannelPublicView(ChannelView.of(c), myStatus);
    }

    @PatchMapping("/{channelId}")
    public ChannelView edit(@PathVariable String channelId, @RequestBody EditChannelRequest req,
                            Authentication auth) {
        UserId user = principal(auth);
        Channel c = channels.edit(user, channelId(channelId), new EditChannelCommand(
            Optional.ofNullable(req.displayName()),
            Optional.ofNullable(req.description()),
            Optional.ofNullable(req.expectationSummary()),
            req.visibility() == null ? Optional.empty() : Optional.of(ChannelDtos.parseVisibility(req.visibility())),
            Optional.ofNullable(req.requireMessageOnReRequest())));
        return ChannelView.of(c);
    }

    @DeleteMapping("/{channelId}")
    public ResponseEntity<Void> delete(@PathVariable String channelId, Authentication auth) {
        channels.delete(principal(auth), channelId(channelId));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public List<ChannelView> search(@RequestParam("q") String q, Authentication auth) {
        principal(auth);
        return channels.searchByName(q).stream().map(ChannelView::of).toList();
    }

    @GetMapping("/directory")
    public List<ChannelView> directory(@RequestParam(value = "page", defaultValue = "0") int page,
                                       Authentication auth) {
        principal(auth);
        return channels.directory(page, 20).stream().map(ChannelView::of).toList();
    }

    // ---- channel files (owner-only) ----

    @GetMapping("/{channelId}/files")
    public List<ChannelFileView> listFiles(@PathVariable String channelId, Authentication auth) {
        return channels.listFiles(principal(auth), channelId(channelId)).stream()
            .map(ChannelFileView::of).toList();
    }

    @PostMapping("/{channelId}/files")
    public ResponseEntity<ChannelFileView> attach(@PathVariable String channelId,
                                                  @RequestBody AttachFileRequest req, Authentication auth) {
        UserId user = principal(auth);
        if (req == null || req.fileId() == null || req.displayLabel() == null)
            throw new ValidationError("fileId and displayLabel are required");
        var cf = channels.attachFile(user, channelId(channelId), fileId(req.fileId()), req.displayLabel());
        return ResponseEntity.status(HttpStatus.CREATED).body(ChannelFileView.of(cf));
    }

    @PatchMapping("/{channelId}/files/{fileId}")
    public ChannelFileView renameLabel(@PathVariable String channelId, @PathVariable String fileId,
                                       @RequestBody RenameLabelRequest req, Authentication auth) {
        UserId user = principal(auth);
        if (req == null || req.displayLabel() == null) throw new ValidationError("displayLabel is required");
        return ChannelFileView.of(channels.renameFileLabel(user, channelId(channelId), fileId(fileId), req.displayLabel()));
    }

    @DeleteMapping("/{channelId}/files/{fileId}")
    public ResponseEntity<Void> detach(@PathVariable String channelId, @PathVariable String fileId,
                                       Authentication auth) {
        channels.detachFile(principal(auth), channelId(channelId), fileId(fileId));
        return ResponseEntity.noContent().build();
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
    private FileId fileId(String raw) {
        try { return FileId.of(raw); } catch (Exception e) { throw new ValidationError("invalid fileId"); }
    }
}
