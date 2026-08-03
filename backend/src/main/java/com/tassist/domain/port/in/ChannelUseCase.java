package com.tassist.domain.port.in;

import com.tassist.domain.model.Channel;
import com.tassist.domain.model.ChannelFile;
import com.tassist.domain.vo.ChannelId;
import com.tassist.domain.vo.ChannelVisibility;
import com.tassist.domain.vo.FileId;
import com.tassist.domain.vo.UserId;
import java.util.List;
import java.util.Optional;

/**
 * Inbound port: channel management, owner side (spec 12.5). Ownership verified in impl (7.4).
 * Channel-file attachment carries an owner-provided citation label; visitors never see the
 * underlying filename (invariant 7.5).
 */
public interface ChannelUseCase {

    Channel create(UserId actingUser, CreateChannelCommand command);

    List<Channel> listOwned(UserId actingUser);

    Channel getByUsername(String username);

    Channel edit(UserId actingUser, ChannelId channelId, EditChannelCommand command);

    void delete(UserId actingUser, ChannelId channelId);

    List<Channel> searchByName(String query);

    List<Channel> directory(int page, int pageSize);

    ChannelFile attachFile(UserId actingUser, ChannelId channelId, FileId fileId, String displayLabel);

    ChannelFile renameFileLabel(UserId actingUser, ChannelId channelId, FileId fileId, String newLabel);

    void detachFile(UserId actingUser, ChannelId channelId, FileId fileId);

    List<ChannelFile> listFiles(UserId actingUser, ChannelId channelId);

    record CreateChannelCommand(
            String username,
            String displayName,
            String description,
            String expectationSummary,
            ChannelVisibility visibility,
            boolean requireMessageOnReRequest
    ) {}

    record EditChannelCommand(
            Optional<String> displayName,
            Optional<String> description,
            Optional<String> expectationSummary,
            Optional<ChannelVisibility> visibility,
            Optional<Boolean> requireMessageOnReRequest
    ) {
        public EditChannelCommand {
            displayName = displayName == null ? Optional.empty() : displayName;
            description = description == null ? Optional.empty() : description;
            expectationSummary = expectationSummary == null ? Optional.empty() : expectationSummary;
            visibility = visibility == null ? Optional.empty() : visibility;
            requireMessageOnReRequest = requireMessageOnReRequest == null ? Optional.empty() : requireMessageOnReRequest;
        }
    }
}
