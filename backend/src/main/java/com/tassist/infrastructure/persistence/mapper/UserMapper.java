package com.tassist.infrastructure.persistence.mapper;

import com.tassist.domain.model.User;
import com.tassist.domain.vo.AuthProvider;
import com.tassist.domain.vo.UserId;
import com.tassist.infrastructure.persistence.entity.UserEntity;
import java.util.Optional;

public final class UserMapper {
    private UserMapper() {}

    public static UserEntity toEntity(User u) {
        UserEntity e = new UserEntity();
        e.setId(u.id().value());
        e.setEmail(u.email());
        e.setDisplayName(u.displayName());
        e.setPasswordHash(u.passwordHash().orElse(null));
        e.setAuthProvider(UserEntity.AuthProviderDb.valueOf(u.authProvider().name()));
        e.setGoogleSubject(u.googleSubject().orElse(null));
        e.setCreatedAt(u.createdAt());
        e.setUpdatedAt(u.updatedAt());
        return e;
    }

    public static User toDomain(UserEntity e) {
        return new User(
            UserId.of(e.getId()),
            e.getEmail(),
            e.getDisplayName(),
            Optional.ofNullable(e.getPasswordHash()),
            AuthProvider.valueOf(e.getAuthProvider().name()),
            Optional.ofNullable(e.getGoogleSubject()),
            e.getCreatedAt(),
            e.getUpdatedAt()
        );
    }
}
