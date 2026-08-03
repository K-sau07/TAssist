package com.tassist.domain.port.out;

import com.tassist.domain.vo.FileType;
import java.util.List;
import java.util.Map;

/**
 * Outbound port for turning raw file bytes into ordered text units (spec §7, §11.1 step 5).
 *
 * <p>One adapter per {@link FileType} lives under {@code infrastructure.parsing.*}. Adding a
 * new file type = adding a new implementation, never editing existing code (Open/Closed, §7).
 * Chunking (§11.2) happens downstream in the ingestion service, not here — the parser only
 * yields provenance-tagged text segments (a page, a slide, a paragraph range).
 *
 * <p>Spreadsheets (XLSX/CSV) do NOT use this port; they take the structured path (§11.3).
 */
public interface DocumentParser {

    /** Which file type this parser handles. */
    FileType supportedType();

    /** Parse raw bytes into ordered segments, each with provenance metadata. */
    List<ParsedSegment> parse(byte[] content);

    /**
     * One ordered unit of parsed text with provenance metadata
     * (e.g. {@code {"page":"4"}}, {@code {"slide":"12","title":"Intro"}}).
     */
    record ParsedSegment(int ordinal, String text, Map<String, String> metadata) {
        public ParsedSegment {
            if (ordinal < 0) throw new IllegalArgumentException("ParsedSegment.ordinal must not be negative");
            if (text == null) throw new IllegalArgumentException("ParsedSegment.text must not be null");
            metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        }
    }
}
