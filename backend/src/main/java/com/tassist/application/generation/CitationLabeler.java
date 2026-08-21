package com.tassist.application.generation;

import com.tassist.domain.vo.FileType;
import java.util.Map;

/**
 * Builds the human-facing citation label for a chunk (§11.8, private-library context):
 * filename + a positional hint derived from chunk metadata. The positional hint format
 * depends on the file type / which metadata keys the Chunker emitted (§11.2).
 */
public final class CitationLabeler {
    private CitationLabeler() {}

    /** e.g. "lecture-04.pdf, page 7", "onboarding.docx, ¶12", "slides.pptx, slide 3", "sales.xlsx, sheet 'Q4'". */
    public static String label(String filename, FileType type, Map<String, String> metadata) {
        String hint = positionalHint(type, metadata == null ? Map.of() : metadata);
        return hint.isEmpty() ? filename : filename + ", " + hint;
    }

    private static String positionalHint(FileType type, Map<String, String> m) {
        return switch (type) {
            case PDF -> {
                String page = m.get("page");
                String part = m.get("part");
                if (page == null) yield "";
                yield part != null ? "page " + page + " (part " + part + ")" : "page " + page;
            }
            case DOCX -> {
                String range = m.get("paragraphRange");
                yield range != null ? "\u00b6" + range : "";
            }
            case PPTX -> {
                String slide = m.get("slide");
                String title = m.get("title");
                if (slide == null) yield "";
                yield title != null && !title.isBlank() ? "slide " + slide + " \u2014 '" + title + "'" : "slide " + slide;
            }
            case MD -> {
                String heading = m.get("heading");
                yield heading != null && !heading.isBlank() ? "\u00a7 " + heading : "";
            }
            case TXT -> {
                String section = m.get("section");
                yield section != null ? "section " + section : "";
            }
            case XLSX, CSV -> {
                String sheet = m.get("sheet");
                yield sheet != null ? "sheet '" + sheet + "'" : "";
            }
        };
    }
}
