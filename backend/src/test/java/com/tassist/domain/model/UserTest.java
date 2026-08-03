package com.tassist.domain.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tassist.domain.vo.AuthProvider;
import com.tassist.domain.vo.UserId;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class UserTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void passwordUser_valid() {
        assertDoesNotThrow(() -> new User(UserId.newId(), "a@b.com", "Alice",
                Optional.of("hash"), AuthProvider.PASSWORD, Optional.empty(), NOW, NOW));
    }

    @Test
    void googleUser_valid() {
        assertDoesNotThrow(() -> new User(UserId.newId(), "a@b.com", "Alice",
                Optional.empty(), AuthProvider.GOOGLE, Optional.of("sub-123"), NOW, NOW));
    }

    @Test
    void passwordUser_missingHash_throws() {
        assertThrows(IllegalArgumentException.class, () -> new User(UserId.newId(), "a@b.com", "Alice",
                Optional.empty(), AuthProvider.PASSWORD, Optional.empty(), NOW, NOW));
    }

    @Test
    void passwordUser_withGoogleSubject_throws() {
        assertThrows(IllegalArgumentException.class, () -> new User(UserId.newId(), "a@b.com", "Alice",
                Optional.of("hash"), AuthProvider.PASSWORD, Optional.of("sub-123"), NOW, NOW));
    }

    @Test
    void googleUser_missingSubject_throws() {
        assertThrows(IllegalArgumentException.class, () -> new User(UserId.newId(), "a@b.com", "Alice",
                Optional.empty(), AuthProvider.GOOGLE, Optional.empty(), NOW, NOW));
    }

    @Test
    void googleUser_withPasswordHash_throws() {
        assertThrows(IllegalArgumentException.class, () -> new User(UserId.newId(), "a@b.com", "Alice",
                Optional.of("hash"), AuthProvider.GOOGLE, Optional.of("sub-123"), NOW, NOW));
    }

    @Test
    void uppercaseEmail_throws() {
        assertThrows(IllegalArgumentException.class, () -> new User(UserId.newId(), "A@B.com", "Alice",
                Optional.of("hash"), AuthProvider.PASSWORD, Optional.empty(), NOW, NOW));
    }

    @Test
    void blankDisplayName_throws() {
        assertThrows(IllegalArgumentException.class, () -> new User(UserId.newId(), "a@b.com", "  ",
                Optional.of("hash"), AuthProvider.PASSWORD, Optional.empty(), NOW, NOW));
    }
}
