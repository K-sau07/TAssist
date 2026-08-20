package com.tassist.infrastructure.persistence.repo;

import com.tassist.infrastructure.persistence.entity.ChunkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.UUID;

public interface ChunkJpaRepository extends JpaRepository<ChunkEntity, UUID> {
    long countByFileId(UUID fileId);
    @Modifying @Query("delete from ChunkEntity c where c.fileId = :fileId")
    void deleteByFileId(@Param("fileId") UUID fileId);
}
