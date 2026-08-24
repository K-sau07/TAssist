package com.tassist.infrastructure.storage;

import com.tassist.domain.error.InternalError;
import com.tassist.domain.port.out.FileStorage;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Local-disk implementation of the FileStorage port (§11.1 step 3, Phase 1).
 * Keys look like "{ownerId}/{fileId}.pdf"; resolved safely under the configured root.
 * Raw bytes live here only for ingestion — never served to the browser or LLM (§7.1).
 */
@Component
@EnableConfigurationProperties(StorageProperties.class)
public class LocalDiskFileStorage implements FileStorage {

    private final Path root;

    public LocalDiskFileStorage(StorageProperties props) {
        this.root = Paths.get(props.getDir()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new InternalError("could not create storage root: " + root);
        }
    }

    @Override
    public void store(String storageKey, byte[] content) {
        Path target = resolve(storageKey);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content);
        } catch (IOException e) {
            throw new InternalError("failed to store file at key " + storageKey);
        }
    }

    @Override
    public InputStream openStream(String storageKey) {
        try {
            return Files.newInputStream(resolve(storageKey));
        } catch (IOException e) {
            throw new InternalError("failed to open file at key " + storageKey);
        }
    }

    @Override
    public byte[] read(String storageKey) {
        try {
            return Files.readAllBytes(resolve(storageKey));
        } catch (IOException e) {
            throw new InternalError("failed to read file at key " + storageKey);
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(resolve(storageKey));
        } catch (IOException e) {
            throw new InternalError("failed to delete file at key " + storageKey);
        }
    }

    @Override
    public boolean exists(String storageKey) {
        return Files.exists(resolve(storageKey));
    }

    /** Resolve a key under root, rejecting path traversal outside the root. */
    private Path resolve(String storageKey) {
        Path resolved = root.resolve(storageKey).normalize();
        if (!resolved.startsWith(root)) {
            throw new InternalError("illegal storage key (path traversal): " + storageKey);
        }
        return resolved;
    }
}
