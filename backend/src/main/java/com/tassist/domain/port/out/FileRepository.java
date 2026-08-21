package com.tassist.domain.port.out;

import com.tassist.domain.model.File;
import com.tassist.domain.vo.FileId;
import com.tassist.domain.vo.UserId;
import java.util.List;
import java.util.Optional;

/** Persistence port for {@link File} (spec §7). */
public interface FileRepository {
    File save(File file);
    Optional<File> findById(FileId id);
    List<File> findByOwner(UserId ownerId);
    Optional<File> findByOwnerAndContentHash(UserId ownerId, String contentHash);
    /** All files with this exact original filename owned by the user (§11.4 @mention resolution). */
    List<File> findByOwnerAndFilename(UserId ownerId, String originalFilename);
    void delete(FileId id);
}
