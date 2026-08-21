package com.tassist.application.folder;

import com.tassist.domain.error.ConflictError;
import com.tassist.domain.error.Forbidden;
import com.tassist.domain.error.NotFoundError;
import com.tassist.domain.error.ValidationError;
import com.tassist.domain.model.File;
import com.tassist.domain.model.Folder;
import com.tassist.domain.model.FolderFile;
import com.tassist.domain.port.in.FolderUseCase;
import com.tassist.domain.port.out.FileRepository;
import com.tassist.domain.port.out.FolderFileRepository;
import com.tassist.domain.port.out.FolderRepository;
import com.tassist.domain.vo.FileId;
import com.tassist.domain.vo.FolderId;
import com.tassist.domain.vo.UserId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Folder CRUD + membership (§12.3). Enforces ownership (§7.4) and unique (owner, name).
 * Flat folders; a file may live in many folders; deleting a folder does not delete files (§8).
 */
@Service
public class FolderService implements FolderUseCase {

    private static final Logger log = LoggerFactory.getLogger(FolderService.class);

    private final FolderRepository folders;
    private final FolderFileRepository folderFiles;
    private final FileRepository files;

    public FolderService(FolderRepository folders, FolderFileRepository folderFiles, FileRepository files) {
        this.folders = folders;
        this.folderFiles = folderFiles;
        this.files = files;
    }

    @Override
    public Folder create(UserId actingUser, String name) {
        String clean = normalizeName(name);
        if (folders.existsByOwnerAndName(actingUser, clean)) {
            throw new ConflictError("a folder named '" + clean + "' already exists");
        }
        Folder folder = new Folder(FolderId.newId(), actingUser, clean, Instant.now());
        Folder saved = folders.save(folder);
        log.info("Folder created: {} '{}' owner={}", saved.id().value(), clean, actingUser.value());
        return saved;
    }

    @Override
    public List<Folder> list(UserId actingUser) {
        return folders.findByOwner(actingUser);
    }

    @Override
    public Folder rename(UserId actingUser, FolderId folderId, String newName) {
        Folder folder = ownedFolder(actingUser, folderId);
        String clean = normalizeName(newName);
        if (!clean.equals(folder.name()) && folders.existsByOwnerAndName(actingUser, clean)) {
            throw new ConflictError("a folder named '" + clean + "' already exists");
        }
        Folder renamed = new Folder(folder.id(), folder.ownerId(), clean, folder.createdAt());
        Folder saved = folders.save(renamed);
        log.info("Folder renamed: {} -> '{}'", folderId.value(), clean);
        return saved;
    }

    @Override
    public void delete(UserId actingUser, FolderId folderId) {
        ownedFolder(actingUser, folderId);
        folders.delete(folderId); // folder_file rows cascade (FK ON DELETE CASCADE); files untouched
        log.info("Folder deleted: {} (files retained)", folderId.value());
    }

    @Override
    public void addFile(UserId actingUser, FolderId folderId, FileId fileId) {
        ownedFolder(actingUser, folderId);
        File file = files.findById(fileId).orElseThrow(() -> new NotFoundError("file not found"));
        if (!file.ownerId().equals(actingUser)) {
            throw new Forbidden("not your file");
        }
        folderFiles.add(new FolderFile(folderId, fileId, Instant.now()));
    }

    @Override
    public void removeFile(UserId actingUser, FolderId folderId, FileId fileId) {
        ownedFolder(actingUser, folderId);
        folderFiles.remove(folderId, fileId);
    }

    @Override
    public List<File> listFiles(UserId actingUser, FolderId folderId) {
        ownedFolder(actingUser, folderId);
        return folderFiles.findFileIdsByFolder(folderId).stream()
            .map(files::findById)
            .flatMap(java.util.Optional::stream)
            .toList();
    }

    // --- helpers ---
    private Folder ownedFolder(UserId actingUser, FolderId folderId) {
        Folder folder = folders.findById(folderId)
            .orElseThrow(() -> new NotFoundError("folder not found"));
        if (!folder.ownerId().equals(actingUser)) {
            throw new Forbidden("not your folder");
        }
        return folder;
    }

    private String normalizeName(String name) {
        if (name == null || name.isBlank()) throw new ValidationError("folder name must not be blank");
        String clean = name.strip();
        if (clean.length() > 128) throw new ValidationError("folder name too long (max 128)");
        return clean;
    }
}
