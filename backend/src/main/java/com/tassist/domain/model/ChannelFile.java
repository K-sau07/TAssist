package com.tassist.domain.model;

import com.tassist.domain.vo.ChannelId;
import com.tassist.domain.vo.FileId;
import java.time.Instant;

/** Join record: which files back a channel, with an owner-provided citation label (spec §8). */
public record ChannelFile(
        ChannelId channelId,
        FileId fileId,
        String displayLabel,
        Instant addedAt
) {
    public ChannelFile {
        if (channelId == null) throw new IllegalArgumentException("ChannelFile.channelId must not be null");
        if (fileId == null) throw new IllegalArgumentException("ChannelFile.fileId must not be null");
        if (displayLabel == null || displayLabel.isBlank())
            throw new IllegalArgumentException("ChannelFile.displayLabel must not be blank");
        if (addedAt == null) throw new IllegalArgumentException("ChannelFile.addedAt must not be null");
    }
}
