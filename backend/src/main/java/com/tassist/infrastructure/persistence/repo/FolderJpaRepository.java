package com.tassist.infrastructure.persistence.repo;

import com.tassist.infrastructure.persistence.entity.FolderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface FolderJpaRepository extends JpaRepository<FolderEntity, UUID> {
    List<FolderEntity> findByOwnerId(UUID ownerId);
    boolean existsByOwnerIdAndName(UUID ownerId, String name);
}
