package com.tassist.application.file;

import com.tassist.domain.vo.FileType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FileTypeResolverTest {

    @Test
    void resolves_by_mime() {
        assertThat(FileTypeResolver.resolve("application/pdf", "x.pdf")).contains(FileType.PDF);
        assertThat(FileTypeResolver.resolve("text/csv", "x.csv")).contains(FileType.CSV);
        assertThat(FileTypeResolver.resolve(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "x.docx"))
            .contains(FileType.DOCX);
    }

    @Test
    void falls_back_to_extension_when_mime_generic() {
        // browsers often send octet-stream for office files
        assertThat(FileTypeResolver.resolve("application/octet-stream", "deck.pptx")).contains(FileType.PPTX);
        assertThat(FileTypeResolver.resolve(null, "notes.md")).contains(FileType.MD);
    }

    @Test
    void unsupported_returns_empty() {
        assertThat(FileTypeResolver.resolve("application/json", "data.json")).isEmpty();
        assertThat(FileTypeResolver.resolve("image/png", "pic.png")).isEmpty();
    }

    @Test
    void strips_mime_params() {
        assertThat(FileTypeResolver.resolve("text/plain; charset=utf-8", "x.txt")).contains(FileType.TXT);
    }

    @Test
    void extension_for_roundtrips() {
        for (FileType t : FileType.values()) {
            assertThat(FileTypeResolver.extensionFor(t)).isNotBlank();
        }
    }
}
