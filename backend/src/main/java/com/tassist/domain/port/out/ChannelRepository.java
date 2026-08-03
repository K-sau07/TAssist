package com.tassist.domain.port.out;

import com.tassist.domain.model.Channel;
import com.tassist.domain.vo.ChannelId;
import com.tassist.domain.vo.UserId;
import java.util.List;
import java.util.Optional;

/** Persistence port for {@link Channel} (spec §7). */
public interface ChannelRepository {
    Channel save(Channel channel);
    Optional<Channel> findById(ChannelId id);
    Optional<Channel> findByUsername(String usernameLowercased);
    List<Channel> findByOwner(UserId ownerId);
    boolean existsByUsername(String usernameLowercased);
    void delete(ChannelId id);
}
