package com.tassist.infrastructure.web.folder;

import com.tassist.domain.error.Unauthenticated;
import com.tassist.domain.error.ValidationError;
import com.tassist.domain.model.Folder;
import com.tassist.domain.port.in.FolderUseCase;
import com.tassist.domain.vo.FileId;
import com.tassist.domain.vo.FolderId;
import com.tassist.domain.vo.UserId;
import com.tassist.infrastructure.web.file.FileDtos.FileView;
import com.tassist.infrastructure.web.folder.FolderDtos.AddFilesRequest;
import com.tassist.infrastructure.web.folder.FolderDtos.CreateFolderRequest;
import com.tassist.infrastructure.web.folder.FolderDtos.FolderView;
import com.tassist.infrastructure.web.folder.FolderDtos.RenameFolderRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/** §12.3 folder endpoints. All require auth; ownership enforced in the service. */
@RestController
@RequestMapping("/api/folders")
public class FolderController {

    private final FolderUseCase folders;

    public FolderController(FolderUseCase folders) { this.folders = folders; }

    @GetMapping
    public ResponseEntity<List<FolderView>> list(Authentication auth) {
        UserId user = principal(auth);
        return ResponseEntity.ok(folders.list(user).stream().map(FolderView::of).toList());
    }

    @PostMapping
    public ResponseEntity<FolderView> create(@RequestBody CreateFolderRequest req, Authentication auth) {
        UserId user = principal(auth);
        if (req == null || req.name() == null) throw new ValidationError("name is required");
        Folder f = folders.create(user, req.name());
        return ResponseEntity.status(HttpStatus.CREATED)
            .location(URI.create("/api/folders/" + f.id().value()))
            .body(FolderView.of(f));
    }

    @PatchMapping("/{folderId}")
    public ResponseEntity<FolderView> rename(@PathVariable String folderId,
                                             @RequestBody RenameFolderRequest req, Authentication auth) {
        UserId user = principal(auth);
        if (req == null || req.name() == null) throw new ValidationError("name is required");
        Folder f = folders.rename(user, folderId(folderId), req.name());
        return ResponseEntity.ok(FolderView.of(f));
    }

    @DeleteMapping("/{folderId}")
    public ResponseEntity<Void> delete(@PathVariable String folderId, Authentication auth) {
        folders.delete(principal(auth), folderId(folderId));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{folderId}/files")
    public ResponseEntity<List<FileView>> listFiles(@PathVariable String folderId, Authentication auth) {
        UserId user = principal(auth);
        return ResponseEntity.ok(
            folders.listFiles(user, folderId(folderId)).stream().map(FileView::of).toList());
    }

    @PostMapping("/{folderId}/files")
    public ResponseEntity<Void> addFiles(@PathVariable String folderId,
                                         @RequestBody AddFilesRequest req, Authentication auth) {
        UserId user = principal(auth);
        if (req == null || req.fileIds() == null || req.fileIds().isEmpty()) {
            throw new ValidationError("fileIds must not be empty");
        }
        FolderId fid = folderId(folderId);
        for (String raw : req.fileIds()) {
            folders.addFile(user, fid, fileId(raw));
        }
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{folderId}/files/{fileId}")
    public ResponseEntity<Void> removeFile(@PathVariable String folderId, @PathVariable String fileId,
                                           Authentication auth) {
        folders.removeFile(principal(auth), folderId(folderId), fileId(fileId));
        return ResponseEntity.noContent().build();
    }

    // --- helpers ---
    private UserId principal(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof UserId userId)) {
            throw new Unauthenticated("authentication required");
        }
        return userId;
    }

    private FolderId folderId(String raw) {
        try { return FolderId.of(raw); }
        catch (IllegalArgumentException e) { throw new ValidationError("invalid folderId"); }
    }

    private FileId fileId(String raw) {
        try { return FileId.of(raw); }
        catch (IllegalArgumentException e) { throw new ValidationError("invalid fileId"); }
    }
}
