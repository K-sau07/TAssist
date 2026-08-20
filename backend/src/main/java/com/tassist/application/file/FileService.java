package com.tassist.application.file;

import com.tassist.domain.error.Forbidden;
import com.tassist.domain.error.NotFoundError;
import com.tassist.domain.model.Chunk;
import com.tassist.domain.model.File;
import com.tassist.domain.port.in.FileUseCase;
import com.tassist.domain.port.out.ChunkRepository;
import com.tassist.domain.port.out.DocumentParser;
import com.tassist.domain.port.out.DocumentParser.ParsedSegment;
import com.tassist.domain.port.out.EmbeddingClient;
import com.tassist.domain.port.out.FileRepository;
import com.tassist.domain.port.out.FileStorage;
import com.tassist.domain.vo.ChunkId;
import com.tassist.domain.vo.FileId;
import com.tassist.domain.vo.FileStatus;
import com.tassist.domain.vo.FileType;
import com.tassist.domain.vo.UserId;
import com.tassist.application.ingest.Chunker;
import com.tassist.application.ingest.TextChunk;
import com.tassist.infrastructure.parsing.ParserRegistry;
import com.tassist.infrastructure.web.error.UploadExceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

/**
 * File ingestion pipeline (§11.1): validate → dedup → store → persist row → parse →
 * chunk → embed → save chunks → READY. Text file types only (PDF/DOCX/PPTX/TXT/MD);
 * spreadsheets (XLSX/CSV) skip chunking and stay at PARSING until Step 6 (D16).
 * On any failure during chunk/embed, the file is marked FAILED with a reason (§11.1).
 */
@Service
public class FileService implements FileUseCase {

    private static final Logger log = LoggerFactory.getLogger(FileService.class);
    private static final long MAX_BYTES = 25L * 1024 * 1024; // 25 MB (§11.1)
    private static final int EMBED_BATCH = 32; // §11.1 step 7

    private final FileRepository files;
    private final FileStorage storage;
    private final ParserRegistry parsers;
    private final Chunker chunker;
    private final EmbeddingClient embeddings;
    private final ChunkRepository chunks;

    public FileService(FileRepository files, FileStorage storage, ParserRegistry parsers,
                       Chunker chunker, EmbeddingClient embeddings, ChunkRepository chunks) {
        this.files = files;
        this.storage = storage;
        this.parsers = parsers;
        this.chunker = chunker;
        this.embeddings = embeddings;
        this.chunks = chunks;
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

        // 5. Route to parser
        DocumentParser parser = parsers.forType(type);
        List<ParsedSegment> segments = parser.parse(bytes);
        int totalChars = segments.stream().mapToInt(s -> s.text().length()).sum();
        log.info("Parsed file {} ({}): {} segments, {} chars. First segment preview: {}",
            fileId.value(), type, segments.size(), totalChars,
            segments.isEmpty() ? "<empty>" : preview(segments.get(0).text()));

        // 6. Spreadsheets take the structured path (§11.3 / Step 6) — no chunking here.
        //    Leave them at PARSING (D16): READY must mean queryable.
        if (type == FileType.XLSX || type == FileType.CSV) {
            log.info("File {} ({}) left at PARSING for spreadsheet structured mode (Step 6).",
                fileId.value(), type);
            return file;
        }

        // 7. Chunk → embed → save → READY. Any failure marks the file FAILED (§11.1).
        try {
            List<TextChunk> textChunks = chunker.chunk(type, segments);
            if (textChunks.isEmpty()) {
                log.warn("File {} produced no chunks; marking READY with zero chunks.", fileId.value());
                return files.save(withStatus(file, FileStatus.READY));
            }
            List<float[]> vectors = embeddings.embedBatch(
                textChunks.stream().map(TextChunk::text).toList());
            if (vectors.size() != textChunks.size()) {
                throw new IllegalStateException("embedding count " + vectors.size()
                    + " != chunk count " + textChunks.size());
            }
            List<Chunk> toSave = new ArrayList<>(textChunks.size());
            for (int i = 0; i < textChunks.size(); i++) {
                TextChunk tc = textChunks.get(i);
                toSave.add(new Chunk(ChunkId.newId(), fileId, tc.ordinal(), tc.text(),
                    tc.metadata(), vectors.get(i)));
            }
            chunks.saveAll(toSave); // atomic, all-or-nothing (§11.1)
            File ready = files.save(withStatus(file, FileStatus.READY));
            log.info("File {} READY: {} chunks embedded ({}-dim).",
                fileId.value(), toSave.size(), embeddings.dimension());
            return ready;
        } catch (RuntimeException e) {
            log.error("Ingestion failed for file {}: {}", fileId.value(), e.toString());
            return files.save(withFailure(file, e.getMessage()));
        }
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
        chunks.deleteByFile(fileId);
        storage.delete(file.storageKey());
        files.delete(fileId);
    }

    private File withStatus(File f, FileStatus status) {
        return new File(f.id(), f.ownerId(), f.originalFilename(), f.type(), f.sizeBytes(),
            f.storageKey(), f.contentHash(), status, f.failureReason(), f.createdAt(), Instant.now());
    }

    private File withFailure(File f, String reason) {
        return new File(f.id(), f.ownerId(), f.originalFilename(), f.type(), f.sizeBytes(),
            f.storageKey(), f.contentHash(), FileStatus.FAILED,
            Optional.ofNullable(reason == null ? "ingestion failed" : reason),
            f.createdAt(), Instant.now());
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
