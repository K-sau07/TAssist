package com.tassist.domain.model;

import com.tassist.domain.vo.ChunkId;
import com.tassist.domain.vo.FileId;
import java.util.Optional;

/** A source reference attached to an assistant message (spec §8). */
public record Citation(
        FileId fileId,
        ChunkId chunkId,
        String displayLabel,
        Optional<String> snippet
) {
    public Citation {
        if (fileId == null) throw new IllegalArgumentException("Citation.fileId must not be null");
        if (chunkId == null) throw new IllegalArgumentException("Citation.chunkId must not be null");
        if (displayLabel == null || displayLabel.isBlank())
            throw new IllegalArgumentException("Citation.displayLabel must not be blank");
        snippet = snippet == null ? Optional.empty() : snippet;
    }
}
