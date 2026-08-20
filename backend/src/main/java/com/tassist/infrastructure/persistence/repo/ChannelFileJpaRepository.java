package com.tassist.infrastructure.persistence.repo;

import com.tassist.infrastructure.persistence.entity.ChannelFileEntity;
import com.tassist.infrastructure.persistence.entity.ChannelFileId;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ChannelFileJpaRepository extends JpaRepository<ChannelFileEntity, ChannelFileId> {
    List<ChannelFileEntity> findByChannelId(UUID channelId);
}
