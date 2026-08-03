package com.tassist.domain.port.out;

import com.tassist.domain.model.ChannelFile;
import com.tassist.domain.vo.ChannelId;
import com.tassist.domain.vo.FileId;
import java.util.List;

/** Persistence port for the channel⇄file join (spec §7). */
public interface ChannelFileRepository {
    ChannelFile add(ChannelFile channelFile);
    void remove(ChannelId channelId, FileId fileId);
    List<ChannelFile> findByChannel(ChannelId channelId);
    List<FileId> findFileIdsByChannel(ChannelId channelId);
}
