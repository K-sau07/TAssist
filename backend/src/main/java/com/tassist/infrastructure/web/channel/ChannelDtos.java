package com.tassist.infrastructure.web.channel;

import com.tassist.domain.model.Channel;
import com.tassist.domain.model.ChannelFile;
import com.tassist.domain.model.Membership;
import com.tassist.domain.vo.ChannelVisibility;
import com.tassist.domain.vo.MembershipStatus;

import java.time.Instant;
import java.util.Optional;

/** Request/response DTOs for channel + membership endpoints (§12.5/§12.6). */
public final class ChannelDtos {
    private ChannelDtos() {}

    public record CreateChannelRequest(String username, String displayName, String description,
                                       String expectationSummary, String visibility,
                                       Boolean requireMessageOnReRequest) {}

    public record EditChannelRequest(String displayName, String description, String expectationSummary,
                                     String visibility, Boolean requireMessageOnReRequest) {}

    /** Public channel view — never exposes owner-internal detail beyond metadata. */
    public record ChannelView(String id, String username, String displayName, String description,
                              String expectationSummary, String visibility, Instant createdAt) {
        public static ChannelView of(Channel c) {
            return new ChannelView(c.id().value().toString(), c.username(), c.displayName(),
                c.description(), c.expectationSummary(), c.visibility().name(), c.createdAt());
        }
    }

    /** Channel view + the requesting user's own membership status (for @{username} view). */
    public record ChannelPublicView(ChannelView channel, String myMembershipStatus) {}

    public record AttachFileRequest(String fileId, String displayLabel) {}
    public record RenameLabelRequest(String displayLabel) {}

    /** Channel file — visitors only ever see displayLabel, never the real filename (§7.5). */
    public record ChannelFileView(String fileId, String displayLabel, Instant addedAt) {
        public static ChannelFileView of(ChannelFile cf) {
            return new ChannelFileView(cf.fileId().value().toString(), cf.displayLabel(), cf.addedAt());
        }
    }

    public record JoinRequest(String message) {}

    public record MembershipView(String id, String userId, String status,
                                 Optional<String> requestMessage, Instant createdAt) {
        public static MembershipView of(Membership m) {
            return new MembershipView(m.id().value().toString(), m.userId().value().toString(),
                m.status().name(), m.requestMessage(), m.createdAt());
        }
    }

    public static ChannelVisibility parseVisibility(String raw) {
        if (raw == null || raw.isBlank()) return ChannelVisibility.PUBLIC;
        try { return ChannelVisibility.valueOf(raw.trim().toUpperCase()); }
        catch (IllegalArgumentException e) {
            throw new com.tassist.domain.error.ValidationError("invalid visibility: " + raw);
        }
    }

    public static MembershipStatus parseStatus(String raw) {
        try { return MembershipStatus.valueOf(raw.trim().toUpperCase()); }
        catch (Exception e) {
            throw new com.tassist.domain.error.ValidationError("invalid status: " + raw);
        }
    }
}
