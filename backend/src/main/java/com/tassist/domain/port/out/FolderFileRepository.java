package com.tassist.domain.port.out;

import com.tassist.domain.model.FolderFile;
import com.tassist.domain.vo.FileId;
import com.tassist.domain.vo.FolderId;
import java.util.List;

/** Persistence port for the folder⇄file join (spec §7). */
public interface FolderFileRepository {
    FolderFile add(FolderFile folderFile);
    void remove(FolderId folderId, FileId fileId);
    List<FileId> findFileIdsByFolder(FolderId folderId);
    List<FolderId> findFolderIdsByFile(FileId fileId);
}
