package com.tassist.infrastructure.persistence.mapper;

import com.tassist.domain.model.File;
import com.tassist.domain.vo.FileId;
import com.tassist.domain.vo.FileStatus;
import com.tassist.domain.vo.FileType;
import com.tassist.domain.vo.UserId;
import com.tassist.infrastructure.persistence.entity.FileEntity;
import java.util.Optional;

public final class FileMapper {
    private FileMapper() {}

    public static FileEntity toEntity(File f) {
        FileEntity e = new FileEntity();
        e.setId(f.id().value());
        e.setOwnerId(f.ownerId().value());
        e.setOriginalFilename(f.originalFilename());
        e.setType(FileEntity.FileTypeDb.valueOf(f.type().name()));
        e.setSizeBytes(f.sizeBytes());
        e.setStorageKey(f.storageKey());
        e.setContentHash(f.contentHash());
        e.setStatus(FileEntity.FileStatusDb.valueOf(f.status().name()));
        e.setFailureReason(f.failureReason().orElse(null));
        e.setCreatedAt(f.createdAt());
        e.setUpdatedAt(f.updatedAt());
        return e;
    }

    public static File toDomain(FileEntity e) {
        return new File(
            FileId.of(e.getId()),
            UserId.of(e.getOwnerId()),
            e.getOriginalFilename(),
            FileType.valueOf(e.getType().name()),
            e.getSizeBytes(),
            e.getStorageKey(),
            e.getContentHash(),
            FileStatus.valueOf(e.getStatus().name()),
            Optional.ofNullable(e.getFailureReason()),
            e.getCreatedAt(),
            e.getUpdatedAt()
        );
    }
}
