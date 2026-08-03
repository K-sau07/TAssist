package com.tassist.domain.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tassist.domain.vo.ChannelId;
import com.tassist.domain.vo.ChatId;
import com.tassist.domain.vo.ChatScope;
import com.tassist.domain.vo.FolderId;
import com.tassist.domain.vo.UserId;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ChatTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void regular_valid() {
        assertDoesNotThrow(() -> new Chat(ChatId.newId(), UserId.newId(), ChatScope.REGULAR,
                Optional.empty(), Optional.empty(), "t", NOW, NOW));
    }

    @Test
    void regular_withFolder_throws() {
        assertThrows(IllegalArgumentException.class, () -> new Chat(ChatId.newId(), UserId.newId(), ChatScope.REGULAR,
                Optional.of(FolderId.newId()), Optional.empty(), "t", NOW, NOW));
    }

    @Test
    void folder_valid() {
        assertDoesNotThrow(() -> new Chat(ChatId.newId(), UserId.newId(), ChatScope.FOLDER,
                Optional.of(FolderId.newId()), Optional.empty(), "t", NOW, NOW));
    }

    @Test
    void folder_missingFolder_throws() {
        assertThrows(IllegalArgumentException.class, () -> new Chat(ChatId.newId(), UserId.newId(), ChatScope.FOLDER,
                Optional.empty(), Optional.empty(), "t", NOW, NOW));
    }

    @Test
    void folder_withChannel_throws() {
        assertThrows(IllegalArgumentException.class, () -> new Chat(ChatId.newId(), UserId.newId(), ChatScope.FOLDER,
                Optional.of(FolderId.newId()), Optional.of(ChannelId.newId()), "t", NOW, NOW));
    }

    @Test
    void channel_valid() {
        assertDoesNotThrow(() -> new Chat(ChatId.newId(), UserId.newId(), ChatScope.CHANNEL,
                Optional.empty(), Optional.of(ChannelId.newId()), "t", NOW, NOW));
    }

    @Test
    void channel_missingChannel_throws() {
        assertThrows(IllegalArgumentException.class, () -> new Chat(ChatId.newId(), UserId.newId(), ChatScope.CHANNEL,
                Optional.empty(), Optional.empty(), "t", NOW, NOW));
    }

    @Test
    void channel_withFolder_throws() {
        assertThrows(IllegalArgumentException.class, () -> new Chat(ChatId.newId(), UserId.newId(), ChatScope.CHANNEL,
                Optional.of(FolderId.newId()), Optional.of(ChannelId.newId()), "t", NOW, NOW));
    }
}
