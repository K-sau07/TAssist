package com.tassist.application.file;

import com.tassist.domain.vo.FileType;

import java.util.Locale;
import java.util.Optional;

/**
 * Maps an upload's declared content-type (with filename-extension fallback) to a FileType.
 * Browsers are inconsistent with Office MIME types, so extension is a reliable backstop.
 */
public final class FileTypeResolver {
    private FileTypeResolver() {}

    public static Optional<FileType> resolve(String contentType, String filename) {
        FileType byMime = fromMime(contentType);
        if (byMime != null) return Optional.of(byMime);
        return Optional.ofNullable(fromExtension(filename));
    }

    private static FileType fromMime(String ct) {
        if (ct == null) return null;
        String c = ct.toLowerCase(Locale.ROOT).trim();
        int semi = c.indexOf(';');
        if (semi > -1) c = c.substring(0, semi).trim();
        return switch (c) {
            case "application/pdf" -> FileType.PDF;
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> FileType.DOCX;
            case "application/vnd.openxmlformats-officedocument.presentationml.presentation" -> FileType.PPTX;
            case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> FileType.XLSX;
            case "text/csv" -> FileType.CSV;
            case "text/markdown", "text/x-markdown" -> FileType.MD;
            case "text/plain" -> FileType.TXT;
            default -> null;
        };
    }

    private static FileType fromExtension(String filename) {
        if (filename == null) return null;
        String f = filename.toLowerCase(Locale.ROOT);
        int dot = f.lastIndexOf('.');
        if (dot < 0) return null;
        return switch (f.substring(dot + 1)) {
            case "pdf" -> FileType.PDF;
            case "docx" -> FileType.DOCX;
            case "pptx" -> FileType.PPTX;
            case "xlsx" -> FileType.XLSX;
            case "csv" -> FileType.CSV;
            case "md", "markdown" -> FileType.MD;
            case "txt" -> FileType.TXT;
            default -> null;
        };
    }

    public static String extensionFor(FileType type) {
        return switch (type) {
            case PDF -> "pdf"; case DOCX -> "docx"; case PPTX -> "pptx";
            case XLSX -> "xlsx"; case CSV -> "csv"; case MD -> "md"; case TXT -> "txt";
        };
    }
}
