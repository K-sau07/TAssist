package com.tassist.domain.port.out;

import com.tassist.domain.model.Folder;
import com.tassist.domain.vo.FolderId;
import com.tassist.domain.vo.UserId;
import java.util.List;
import java.util.Optional;

/** Persistence port for {@link Folder} (spec §7). */
public interface FolderRepository {
    Folder save(Folder folder);
    Optional<Folder> findById(FolderId id);
    List<Folder> findByOwner(UserId ownerId);
    boolean existsByOwnerAndName(UserId ownerId, String name);
    void delete(FolderId id);
}
