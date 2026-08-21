package com.tassist.infrastructure.web.file;

import com.tassist.domain.model.File;
import com.tassist.domain.port.in.QuotaUseCase;
import com.tassist.domain.port.in.FileUseCase;
import com.tassist.domain.port.in.FileUseCase.UploadCommand;
import com.tassist.domain.vo.FileId;
import com.tassist.domain.vo.UserId;
import com.tassist.infrastructure.web.file.FileDtos.FileView;
import com.tassist.infrastructure.web.file.FileDtos.StatusView;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.tassist.domain.error.Unauthenticated;
import com.tassist.domain.error.ValidationError;

import java.io.IOException;
import java.net.URI;
import java.util.List;

/** §12.2 file endpoints. All require auth; ownership enforced in the service. */
@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileUseCase files;
    private final QuotaUseCase quota;

    public FileController(FileUseCase files, QuotaUseCase quota) {
        this.quota = quota; this.files = files; }

    @PostMapping
    public ResponseEntity<FileView> upload(@RequestParam("file") MultipartFile multipart,
                                           Authentication auth) throws IOException {
        UserId user = principal(auth);
        if (multipart == null || multipart.isEmpty()) {
            throw new ValidationError("file part is required");
        }
        byte[] bytes = multipart.getBytes();
        // §16.2: enforce monthly file-count + total-storage quota before writing.
        quota.checkUploadAllowed(user, bytes.length);
        UploadCommand cmd = new UploadCommand(
            multipart.getOriginalFilename(),
            multipart.getContentType(),
            bytes);
        File file = files.upload(user, cmd);
        quota.recordUpload(user, bytes.length);
        return ResponseEntity.status(HttpStatus.CREATED)
            .location(URI.create("/api/files/" + file.id().value()))
            .body(FileView.of(file));
    }

    @GetMapping
    public ResponseEntity<List<FileView>> list(Authentication auth) {
        UserId user = principal(auth);
        return ResponseEntity.ok(files.list(user).stream().map(FileView::of).toList());
    }

    @GetMapping("/{fileId}")
    public ResponseEntity<FileView> get(@PathVariable String fileId, Authentication auth) {
        UserId user = principal(auth);
        return ResponseEntity.ok(FileView.of(files.get(user, FileId.of(fileId))));
    }

    @GetMapping("/{fileId}/status")
    public ResponseEntity<StatusView> status(@PathVariable String fileId, Authentication auth) {
        UserId user = principal(auth);
        return ResponseEntity.ok(StatusView.of(files.get(user, FileId.of(fileId))));
    }

    @DeleteMapping("/{fileId}")
    public ResponseEntity<Void> delete(@PathVariable String fileId, Authentication auth) {
        UserId user = principal(auth);
        files.delete(user, FileId.of(fileId));
        return ResponseEntity.noContent().build();
    }

    private UserId principal(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof UserId userId)) {
            throw new Unauthenticated("authentication required");
        }
        return userId;
    }
}
