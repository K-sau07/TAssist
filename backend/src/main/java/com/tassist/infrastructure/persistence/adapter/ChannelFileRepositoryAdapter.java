package com.tassist.infrastructure.persistence.adapter;

import com.tassist.domain.model.ChannelFile;
import com.tassist.domain.port.out.ChannelFileRepository;
import com.tassist.domain.vo.ChannelId;
import com.tassist.domain.vo.FileId;
import com.tassist.infrastructure.persistence.entity.ChannelFileId;
import com.tassist.infrastructure.persistence.mapper.ChannelFileMapper;
import com.tassist.infrastructure.persistence.repo.ChannelFileJpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public class ChannelFileRepositoryAdapter implements ChannelFileRepository {
    private final ChannelFileJpaRepository jpa;
    public ChannelFileRepositoryAdapter(ChannelFileJpaRepository jpa) { this.jpa = jpa; }

    @Override public ChannelFile add(ChannelFile cf) { return ChannelFileMapper.toDomain(jpa.save(ChannelFileMapper.toEntity(cf))); }
    @Override public void remove(ChannelId channelId, FileId fileId) { jpa.deleteById(new ChannelFileId(channelId.value(), fileId.value())); }
    @Override public List<ChannelFile> findByChannel(ChannelId channelId) { return jpa.findByChannelId(channelId.value()).stream().map(ChannelFileMapper::toDomain).toList(); }
    @Override public List<FileId> findFileIdsByChannel(ChannelId channelId) { return jpa.findByChannelId(channelId.value()).stream().map(e -> FileId.of(e.getFileId())).toList(); }
}
