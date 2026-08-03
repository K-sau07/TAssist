package com.tassist.domain.model;

import com.tassist.domain.vo.FileId;
import com.tassist.domain.vo.FolderId;
import java.time.Instant;

/** Join record placing a file into a folder (spec §8). */
public record FolderFile(FolderId folderId, FileId fileId, Instant addedAt) {
    public FolderFile {
        if (folderId == null) throw new IllegalArgumentException("FolderFile.folderId must not be null");
        if (fileId == null) throw new IllegalArgumentException("FolderFile.fileId must not be null");
        if (addedAt == null) throw new IllegalArgumentException("FolderFile.addedAt must not be null");
    }
}
