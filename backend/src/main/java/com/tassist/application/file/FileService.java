package com.tassist.application.file;

import com.tassist.domain.error.Forbidden;
import com.tassist.domain.error.NotFoundError;
import com.tassist.domain.model.File;
import com.tassist.domain.port.in.FileUseCase;
import com.tassist.domain.port.out.DocumentParser;
import com.tassist.domain.port.out.DocumentParser.ParsedSegment;
import com.tassist.domain.port.out.FileRepository;
import com.tassist.domain.port.out.FileStorage;
import com.tassist.domain.vo.FileId;
import com.tassist.domain.vo.FileStatus;
import com.tassist.domain.vo.FileType;
import com.tassist.domain.vo.UserId;
import com.tassist.infrastructure.parsing.ParserRegistry;
import com.tassist.infrastructure.web.error.UploadExceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

/**
 * File ingestion pipeline (§11.1). Step 4 scope: validate → dedup → store → persist row
 * → parse → log. Chunking/embedding/READY flip come in Step 5.
 */
@Service
public class FileService implements FileUseCase {

    private static final Logger log = LoggerFactory.getLogger(FileService.class);
    private static final long MAX_BYTES = 25L * 1024 * 1024; // 25 MB (§11.1)

    private final FileRepository files;
    private final FileStorage storage;
    private final ParserRegistry parsers;

    public FileService(FileRepository files, FileStorage storage, ParserRegistry parsers) {
        this.files = files;
        this.storage = storage;
        this.parsers = parsers;
    }

    @Override
    public File upload(UserId actingUser, UploadCommand command) {
        // 1. Validate type + size
        FileType type = FileTypeResolver.resolve(command.declaredContentType(), command.originalFilename())
            .orElseThrow(() -> new UploadExceptions.UnsupportedMediaType(
                "unsupported content type: " + command.declaredContentType()));
        byte[] bytes = command.content();
        if (bytes.length > MAX_BYTES) {
            throw new UploadExceptions.PayloadTooLarge("file exceeds 25 MB limit");
        }
        String filename = sanitize(command.originalFilename());

        // 2. Dedup by (ownerId, contentHash)
        String contentHash = sha256(bytes);
        Optional<File> existing = files.findByOwnerAndContentHash(actingUser, contentHash);
        if (existing.isPresent()) {
            log.info("Upload dedup hit: user={} hash={} -> existing file {}",
                actingUser.value(), contentHash, existing.get().id().value());
            return existing.get();
        }

        // 3. Persist raw bytes
        FileId fileId = FileId.newId();
        String storageKey = actingUser.value() + "/" + fileId.value() + "." + FileTypeResolver.extensionFor(type);
        storage.store(storageKey, bytes);

        // 4. Create file row (UPLOADING -> PARSING)
        Instant now = Instant.now();
        File file = new File(fileId, actingUser, filename, type, bytes.length, storageKey,
            contentHash, FileStatus.UPLOADING, Optional.empty(), now, now);
        file = files.save(file);
        file = files.save(withStatus(file, FileStatus.PARSING));

        // 5. Route to parser + log extracted text (Step 4 stops here; chunking is Step 5)
        DocumentParser parser = parsers.forType(type);
        List<ParsedSegment> segments = parser.parse(bytes);
        int totalChars = segments.stream().mapToInt(s -> s.text().length()).sum();
        log.info("Parsed file {} ({}): {} segments, {} chars. First segment preview: {}",
            fileId.value(), type, segments.size(), totalChars,
            segments.isEmpty() ? "<empty>" : preview(segments.get(0).text()));

        return file;
    }

    @Override
    public List<File> list(UserId actingUser) {
        return files.findByOwner(actingUser);
    }

    @Override
    public File get(UserId actingUser, FileId fileId) {
        File file = files.findById(fileId)
            .orElseThrow(() -> new NotFoundError("file not found"));
        if (!file.ownerId().equals(actingUser)) {
            throw new Forbidden("not your file");
        }
        return file;
    }

    @Override
    public void delete(UserId actingUser, FileId fileId) {
        File file = get(actingUser, fileId); // ownership check
        storage.delete(file.storageKey());
        files.delete(fileId);
    }

    private File withStatus(File f, FileStatus status) {
        return new File(f.id(), f.ownerId(), f.originalFilename(), f.type(), f.sizeBytes(),
            f.storageKey(), f.contentHash(), status, f.failureReason(), f.createdAt(), Instant.now());
    }

    private String sanitize(String name) {
        String base = name.replaceAll(".*[/\\\\]", "").strip();
        return base.isEmpty() ? "file" : base;
    }

    private String sha256(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private String preview(String text) {
        String t = text.strip().replaceAll("\\s+", " ");
        return t.length() > 120 ? t.substring(0, 120) + "..." : t;
    }
}
