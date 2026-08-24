package com.tassist.infrastructure.storage;

import com.tassist.domain.error.InternalError;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Unit test for the local-disk FileStorage adapter (no Spring context). */
class LocalDiskFileStorageTest {

    private LocalDiskFileStorage storage(Path dir) {
        StorageProperties p = new StorageProperties();
        p.setDir(dir.toString());
        return new LocalDiskFileStorage(p);
    }

    @Test
    void store_read_exists_delete_roundtrip(@TempDir Path dir) {
        LocalDiskFileStorage s = storage(dir);
        String key = "owner-1/file-1.pdf";
        byte[] data = "hello bytes".getBytes();

        assertThat(s.exists(key)).isFalse();
        s.store(key, data);
        assertThat(s.exists(key)).isTrue();
        assertThat(s.read(key)).isEqualTo(data);

        s.delete(key);
        assertThat(s.exists(key)).isFalse();
    }

    @Test
    void delete_missing_key_is_noop(@TempDir Path dir) {
        LocalDiskFileStorage s = storage(dir);
        s.delete("owner-x/nope.txt"); // must not throw
        assertThat(s.exists("owner-x/nope.txt")).isFalse();
    }

    @Test
    void rejects_path_traversal(@TempDir Path dir) {
        LocalDiskFileStorage s = storage(dir);
        assertThatThrownBy(() -> s.store("../escape.txt", new byte[]{1}))
            .isInstanceOf(InternalError.class);
    }

    @Test
    void openStream_reads_same_bytes(@TempDir Path dir) throws Exception {
        LocalDiskFileStorage s = storage(dir);
        String key = "owner-2/doc.txt";
        s.store(key, "streamed".getBytes());
        try (var in = s.openStream(key)) {
            assertThat(new String(in.readAllBytes())).isEqualTo("streamed");
        }
    }
}
