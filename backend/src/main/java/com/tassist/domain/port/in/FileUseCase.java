package com.tassist.domain.port.in;

import com.tassist.domain.model.File;
import com.tassist.domain.vo.FileId;
import com.tassist.domain.vo.UserId;
import java.util.List;

/**
 * Inbound port: file upload / listing / deletion (spec §12.2). The upload command carries
 * raw bytes only as far as the ingestion pipeline (Steps 4–6); they never leave the backend
 * afterwards (invariant §7.1). Ownership is verified inside the implementation (§7.4).
 */
public interface FileUseCase {

    /** Upload (or dedup to an existing file by content hash). Kicks off ingestion. */
    File upload(UserId actingUser, UploadCommand command);

    List<File> list(UserId actingUser);

    File get(UserId actingUser, FileId fileId);

    /** Hard delete (Phase 1): removes file, chunks, spreadsheet data, raw bytes, join rows. */
    void delete(UserId actingUser, FileId fileId);

    /** Raw upload input. {@code declaredContentType} is validated against the 7 supported types. */
    record UploadCommand(String originalFilename, String declaredContentType, byte[] content) {
        public UploadCommand {
            if (originalFilename == null || originalFilename.isBlank())
                throw new IllegalArgumentException("UploadCommand.originalFilename must not be blank");
            if (content == null || content.length == 0)
                throw new IllegalArgumentException("UploadCommand.content must not be empty");
        }
    }
}
