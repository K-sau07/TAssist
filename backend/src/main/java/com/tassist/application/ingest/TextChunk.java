package com.tassist.application.ingest;

import java.util.Map;

/** One chunk ready to embed + persist: ordinal within the file, text, provenance metadata (§11.2). */
public record TextChunk(int ordinal, String text, Map<String, String> metadata) {
    public TextChunk {
        if (text == null) throw new IllegalArgumentException("TextChunk.text must not be null");
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
