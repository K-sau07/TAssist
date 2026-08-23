package com.tassist.application.channel;

import com.tassist.domain.error.ConflictError;
import com.tassist.domain.error.Forbidden;
import com.tassist.domain.error.NotFoundError;
import com.tassist.domain.error.ValidationError;
import com.tassist.domain.model.Channel;
import com.tassist.domain.model.ChannelFile;
import com.tassist.domain.port.in.ChannelUseCase;
import com.tassist.domain.port.out.ChannelFileRepository;
import com.tassist.domain.port.out.ChannelRepository;
import com.tassist.domain.port.out.MembershipRepository;
import com.tassist.domain.model.Membership;
import com.tassist.domain.vo.MembershipStatus;
import com.tassist.domain.port.out.FileRepository;
import com.tassist.domain.vo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/** Channel management, owner side (§12.5). Ownership verified here (§7.4); filenames hidden behind displayLabel (§7.5). */
@Service
public class ChannelService implements ChannelUseCase {

    private static final Logger log = LoggerFactory.getLogger(ChannelService.class);
    private static final int MAX_PAGE_SIZE = 50;

    private final ChannelRepository channels;
    private final ChannelFileRepository channelFiles;
    private final FileRepository files;
    private final MembershipRepository memberships;

    public ChannelService(ChannelRepository channels, ChannelFileRepository channelFiles, FileRepository files, MembershipRepository memberships) {
        this.channels = channels;
        this.channelFiles = channelFiles;
        this.files = files;
        this.memberships = memberships;
    }

    @Override
    @Transactional
    public Channel create(UserId actingUser, CreateChannelCommand cmd) {
        String username = cmd.username() == null ? null : cmd.username().trim().toLowerCase();
        if (channels.existsByUsername(username))
            throw new ConflictError("channel username already taken: " + username);
        Instant now = Instant.now();
        Channel channel = new Channel(ChannelId.newId(), actingUser, username, cmd.displayName(),
            cmd.description(), cmd.expectationSummary(), cmd.visibility(), java.util.Optional.empty(),
            cmd.requireMessageOnReRequest(), now, now);
        Channel saved = channels.save(channel);
        log.info("Channel created: {} @{} owner={}", saved.id().value(), username, actingUser.value());
        return saved;
    }

    @Override
    public List<Channel> listOwned(UserId actingUser) {
        return channels.findByOwner(actingUser);
    }

    @Override
    public List<Channel> listJoined(UserId actingUser) {
        return memberships.findByUserAndStatus(actingUser, MembershipStatus.APPROVED).stream()
            .map(Membership::channelId)
            .map(channels::findById)
            .filter(java.util.Optional::isPresent).map(java.util.Optional::get)
            .toList();
    }

    @Override
    public Channel getByUsername(String username) {
        return channels.findByUsername(username == null ? null : username.trim().toLowerCase())
            .orElseThrow(() -> new NotFoundError("channel not found"));
    }

    @Override
    @Transactional
    public Channel edit(UserId actingUser, ChannelId channelId, EditChannelCommand cmd) {
        Channel c = owned(actingUser, channelId);
        Channel updated = new Channel(c.id(), c.ownerId(), c.username(),
            cmd.displayName().orElse(c.displayName()),
            cmd.description().orElse(c.description()),
            cmd.expectationSummary().orElse(c.expectationSummary()),
            cmd.visibility().orElse(c.visibility()),
            c.avatarKey(),
            cmd.requireMessageOnReRequest().orElse(c.requireMessageOnReRequest()),
            c.createdAt(), Instant.now());
        return channels.save(updated);
    }

    @Override
    @Transactional
    public void delete(UserId actingUser, ChannelId channelId) {
        owned(actingUser, channelId);
        channels.delete(channelId); // cascade of memberships/files/chats handled by FK ON DELETE CASCADE
        log.info("Channel deleted: {}", channelId.value());
    }

    @Override
    public List<Channel> searchByName(String query) {
        if (query == null || query.isBlank()) return List.of();
        return channels.searchByUsernameOrDisplayName(query.trim().toLowerCase(), MAX_PAGE_SIZE);
    }

    @Override
    public List<Channel> directory(int page, int pageSize) {
        int size = Math.max(1, Math.min(pageSize <= 0 ? MAX_PAGE_SIZE : pageSize, MAX_PAGE_SIZE));
        return channels.findPublic(Math.max(0, page), size);
    }

    @Override
    @Transactional
    public ChannelFile attachFile(UserId actingUser, ChannelId channelId, FileId fileId, String displayLabel) {
        owned(actingUser, channelId);
        var file = files.findById(fileId).orElseThrow(() -> new NotFoundError("file not found"));
        if (!file.ownerId().equals(actingUser))
            throw new Forbidden("cannot attach a file you don't own");
        if (displayLabel == null || displayLabel.isBlank())
            throw new ValidationError("displayLabel is required (visitors never see the real filename)");
        ChannelFile cf = channelFiles.add(new ChannelFile(channelId, fileId, displayLabel.trim(), Instant.now()));
        log.info("Channel {} attached file {} as \"{}\"", channelId.value(), fileId.value(), displayLabel);
        return cf;
    }

    @Override
    @Transactional
    public ChannelFile renameFileLabel(UserId actingUser, ChannelId channelId, FileId fileId, String newLabel) {
        owned(actingUser, channelId);
        if (newLabel == null || newLabel.isBlank()) throw new ValidationError("newLabel is required");
        channelFiles.findByChannel(channelId).stream()
            .filter(cf -> cf.fileId().equals(fileId)).findFirst()
            .orElseThrow(() -> new NotFoundError("file not attached to this channel"));
        // upsert via add (same PK channelId+fileId) with the new label
        return channelFiles.add(new ChannelFile(channelId, fileId, newLabel.trim(), Instant.now()));
    }

    @Override
    @Transactional
    public void detachFile(UserId actingUser, ChannelId channelId, FileId fileId) {
        owned(actingUser, channelId);
        channelFiles.remove(channelId, fileId);
        log.info("Channel {} detached file {}", channelId.value(), fileId.value());
    }

    @Override
    public List<ChannelFile> listFiles(UserId actingUser, ChannelId channelId) {
        owned(actingUser, channelId);
        return channelFiles.findByChannel(channelId);
    }

    private Channel owned(UserId actingUser, ChannelId channelId) {
        Channel c = channels.findById(channelId).orElseThrow(() -> new NotFoundError("channel not found"));
        if (!c.ownerId().equals(actingUser)) throw new Forbidden("not your channel");
        return c;
    }
}
