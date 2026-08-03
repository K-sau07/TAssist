package com.tassist.domain.port.out;

import java.io.InputStream;

/**
 * Outbound port for raw file byte storage (spec §7, §11.1 step 3).
 *
 * <p>Adapter lives in {@code infrastructure.storage} (local disk in Phase 1). Raw bytes
 * live behind this port and are read only during ingestion — never sent onward to the LLM
 * or the browser (architectural invariant §7.1).
 */
public interface FileStorage {

    /** Persist bytes under {@code storageKey} (e.g. {@code {ownerId}/{fileId}.pdf}). */
    void store(String storageKey, byte[] content);

    /** Open the stored bytes for reading (ingestion use only). */
    InputStream openStream(String storageKey);

    /** Read the stored bytes fully (ingestion use only). */
    byte[] read(String storageKey);

    /** Remove stored bytes. Idempotent: deleting a missing key is a no-op. */
    void delete(String storageKey);

    /** Whether an object exists for this key. */
    boolean exists(String storageKey);
}
