package com.tassist.infrastructure.persistence.repo;

import com.tassist.infrastructure.persistence.entity.FileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FileJpaRepository extends JpaRepository<FileEntity, UUID> {
    List<FileEntity> findByOwnerId(UUID ownerId);
    Optional<FileEntity> findByOwnerIdAndContentHash(UUID ownerId, String contentHash);
    List<FileEntity> findByOwnerIdAndOriginalFilename(UUID ownerId, String originalFilename);
}
