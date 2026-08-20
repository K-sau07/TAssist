package com.tassist.infrastructure.persistence.repo;

import com.tassist.infrastructure.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByEmail(String email);
    Optional<UserEntity> findByGoogleSubject(String googleSubject);
    boolean existsByEmail(String email);
}
