package com.tassist.infrastructure.persistence.repo;

import com.tassist.infrastructure.persistence.entity.FolderFileEntity;
import com.tassist.infrastructure.persistence.entity.FolderFileId;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface FolderFileJpaRepository extends JpaRepository<FolderFileEntity, FolderFileId> {
    List<FolderFileEntity> findByFolderId(UUID folderId);
    List<FolderFileEntity> findByFileId(UUID fileId);
}
