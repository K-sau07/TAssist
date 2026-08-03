package com.tassist.domain.model;

import com.tassist.domain.vo.ChannelId;
import com.tassist.domain.vo.ChatId;
import com.tassist.domain.vo.ChatScope;
import com.tassist.domain.vo.FolderId;
import com.tassist.domain.vo.UserId;
import java.time.Instant;
import java.util.Optional;

/**
 * A conversation owned by a user (spec §8).
 *
 * <p>Structural scope invariants (enforced here):
 * <ul>
 *   <li>{@code REGULAR} ⇒ both {@code folderId} and {@code channelId} empty.</li>
 *   <li>{@code FOLDER}  ⇒ {@code folderId} present, {@code channelId} empty.</li>
 *   <li>{@code CHANNEL} ⇒ {@code channelId} present, {@code folderId} empty.</li>
 * </ul>
 *
 * <p>Behavioural scope rules that need external state — that a {@code FOLDER} chat's folder
 * is owned by {@code ownerId}, and that a {@code CHANNEL} chat's owner is an approved member —
 * are enforced in the application layer (spec invariant §7.4), not here.
 */
public record Chat(
        ChatId id,
        UserId ownerId,
        ChatScope scope,
        Optional<FolderId> folderId,
        Optional<ChannelId> channelId,
        String title,
        Instant createdAt,
        Instant updatedAt
) {
    public Chat {
        if (id == null) throw new IllegalArgumentException("Chat.id must not be null");
        if (ownerId == null) throw new IllegalArgumentException("Chat.ownerId must not be null");
        if (scope == null) throw new IllegalArgumentException("Chat.scope must not be null");
        folderId = folderId == null ? Optional.empty() : folderId;
        channelId = channelId == null ? Optional.empty() : channelId;
        if (title == null) throw new IllegalArgumentException("Chat.title must not be null");
        if (createdAt == null) throw new IllegalArgumentException("Chat.createdAt must not be null");
        if (updatedAt == null) throw new IllegalArgumentException("Chat.updatedAt must not be null");

        switch (scope) {
            case REGULAR -> {
                if (folderId.isPresent() || channelId.isPresent())
                    throw new IllegalArgumentException("REGULAR chat must have neither folderId nor channelId");
            }
            case FOLDER -> {
                if (folderId.isEmpty())
                    throw new IllegalArgumentException("FOLDER chat must have a folderId");
                if (channelId.isPresent())
                    throw new IllegalArgumentException("FOLDER chat must not have a channelId");
            }
            case CHANNEL -> {
                if (channelId.isEmpty())
                    throw new IllegalArgumentException("CHANNEL chat must have a channelId");
                if (folderId.isPresent())
                    throw new IllegalArgumentException("CHANNEL chat must not have a folderId");
            }
        }
    }
}
