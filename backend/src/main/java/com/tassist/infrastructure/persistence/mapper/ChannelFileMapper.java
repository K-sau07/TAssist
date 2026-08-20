package com.tassist.infrastructure.persistence.mapper;

import com.tassist.domain.model.ChannelFile;
import com.tassist.domain.vo.ChannelId;
import com.tassist.domain.vo.FileId;
import com.tassist.infrastructure.persistence.entity.ChannelFileEntity;

public final class ChannelFileMapper {
    private ChannelFileMapper() {}

    public static ChannelFileEntity toEntity(ChannelFile c) {
        ChannelFileEntity e = new ChannelFileEntity();
        e.setChannelId(c.channelId().value());
        e.setFileId(c.fileId().value());
        e.setDisplayLabel(c.displayLabel());
        e.setAddedAt(c.addedAt());
        return e;
    }

    public static ChannelFile toDomain(ChannelFileEntity e) {
        return new ChannelFile(ChannelId.of(e.getChannelId()), FileId.of(e.getFileId()), e.getDisplayLabel(), e.getAddedAt());
    }
}
