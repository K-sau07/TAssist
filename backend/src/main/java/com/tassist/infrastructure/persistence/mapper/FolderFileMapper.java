package com.tassist.infrastructure.persistence.mapper;

import com.tassist.domain.model.FolderFile;
import com.tassist.domain.vo.FileId;
import com.tassist.domain.vo.FolderId;
import com.tassist.infrastructure.persistence.entity.FolderFileEntity;

public final class FolderFileMapper {
    private FolderFileMapper() {}

    public static FolderFileEntity toEntity(FolderFile f) {
        FolderFileEntity e = new FolderFileEntity();
        e.setFolderId(f.folderId().value());
        e.setFileId(f.fileId().value());
        e.setAddedAt(f.addedAt());
        return e;
    }

    public static FolderFile toDomain(FolderFileEntity e) {
        return new FolderFile(FolderId.of(e.getFolderId()), FileId.of(e.getFileId()), e.getAddedAt());
    }
}
