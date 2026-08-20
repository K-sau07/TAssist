package com.tassist.infrastructure.web.file;

import com.tassist.domain.model.File;

/** §12.2 file response shapes. Never exposes raw bytes or storageKey (§7.1). */
public final class FileDtos {
    private FileDtos() {}

    public record FileView(String id, String originalFilename, String type,
                           long sizeBytes, String status, String failureReason,
                           String createdAt, String updatedAt) {
        public static FileView of(File f) {
            return new FileView(
                f.id().value().toString(),
                f.originalFilename(),
                f.type().name(),
                f.sizeBytes(),
                f.status().name(),
                f.failureReason().orElse(null),
                f.createdAt().toString(),
                f.updatedAt().toString()
            );
        }
    }

    public record StatusView(String id, String status, String failureReason) {
        public static StatusView of(File f) {
            return new StatusView(f.id().value().toString(), f.status().name(),
                f.failureReason().orElse(null));
        }
    }
}
