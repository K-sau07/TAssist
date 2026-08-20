package com.tassist.infrastructure.persistence.mapper;

import com.tassist.domain.model.Folder;
import com.tassist.domain.vo.FolderId;
import com.tassist.domain.vo.UserId;
import com.tassist.infrastructure.persistence.entity.FolderEntity;
import java.time.Instant;

public final class FolderMapper {
    private FolderMapper() {}

    public static FolderEntity toEntity(Folder f) {
        FolderEntity e = new FolderEntity();
        e.setId(f.id().value());
        e.setOwnerId(f.ownerId().value());
        e.setName(f.name());
        e.setCreatedAt(f.createdAt());
        e.setUpdatedAt(f.createdAt());
        return e;
    }

    public static Folder toDomain(FolderEntity e) {
        return new Folder(FolderId.of(e.getId()), UserId.of(e.getOwnerId()), e.getName(), e.getCreatedAt());
    }
}
