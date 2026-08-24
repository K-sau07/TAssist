package com.tassist.domain.model;

import com.tassist.domain.vo.ChannelId;
import com.tassist.domain.vo.ChannelVisibility;
import com.tassist.domain.vo.UserId;
import java.time.Instant;
import java.util.Optional;
import java.util.regex.Pattern;

/** A public Q&A surface owned by a user (spec §8). {@code username} is globally unique. */
public record Channel(
        ChannelId id,
        UserId ownerId,
        String username,
        String displayName,
        String description,
        String expectationSummary,
        ChannelVisibility visibility,
        Optional<String> avatarKey,
        boolean requireMessageOnReRequest,
        boolean groupChatEnabled,
        Instant createdAt,
        Instant updatedAt
) {
    private static final Pattern USERNAME = Pattern.compile("[a-z0-9-]{3,32}");

    public Channel {
        if (id == null) throw new IllegalArgumentException("Channel.id must not be null");
        if (ownerId == null) throw new IllegalArgumentException("Channel.ownerId must not be null");
        if (username == null || !USERNAME.matcher(username).matches())
            throw new IllegalArgumentException("Channel.username must be [a-z0-9-], 3-32 chars, lowercased");
        if (displayName == null || displayName.isBlank())
            throw new IllegalArgumentException("Channel.displayName must not be blank");
        description = description == null ? "" : description;
        expectationSummary = expectationSummary == null ? "" : expectationSummary;
        if (visibility == null) throw new IllegalArgumentException("Channel.visibility must not be null");
        avatarKey = avatarKey == null ? Optional.empty() : avatarKey;
        if (createdAt == null) throw new IllegalArgumentException("Channel.createdAt must not be null");
        if (updatedAt == null) throw new IllegalArgumentException("Channel.updatedAt must not be null");
    }

    /** Returns a copy with the group room toggled, updatedAt bumped (owner setting, 02_MESSAGING_SPEC §2.5). */
    public Channel withGroupChatEnabled(boolean enabled) {
        return new Channel(id, ownerId, username, displayName, description, expectationSummary,
            visibility, avatarKey, requireMessageOnReRequest, enabled, createdAt, java.time.Instant.now());
    }
}
