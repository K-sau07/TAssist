package com.tassist.domain.port.in;

import com.tassist.domain.model.File;
import com.tassist.domain.model.Folder;
import com.tassist.domain.vo.FileId;
import com.tassist.domain.vo.FolderId;
import com.tassist.domain.vo.UserId;
import java.util.List;

/** Inbound port: folder CRUD + membership (spec §12.3). Ownership verified in impl (§7.4). */
public interface FolderUseCase {
    Folder create(UserId actingUser, String name);
    List<Folder> list(UserId actingUser);
    void delete(UserId actingUser, FolderId folderId);
    void addFile(UserId actingUser, FolderId folderId, FileId fileId);
    void removeFile(UserId actingUser, FolderId folderId, FileId fileId);
    List<File> listFiles(UserId actingUser, FolderId folderId);
}
