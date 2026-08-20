package com.tassist.application.ingest;

import com.tassist.domain.port.out.DocumentParser.ParsedSegment;
import com.tassist.domain.vo.FileType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns parser segments into embeddable chunks per §11.2. Target ~500 tokens, 50 overlap.
 * Token count estimated as characters/4. Text-only file types; XLSX/CSV never reach here
 * (spreadsheet path, §11.3 / Step 6).
 */
@Component
public class Chunker {

    private static final int TARGET_TOKENS = 500;
    private static final int OVERLAP_TOKENS = 50;
    private static final int CHARS_PER_TOKEN = 4;
    private static final int TARGET_CHARS = TARGET_TOKENS * CHARS_PER_TOKEN;   // 2000
    private static final int OVERLAP_CHARS = OVERLAP_TOKENS * CHARS_PER_TOKEN; // 200
    private static final int PPTX_SPLIT_CHARS = 800 * CHARS_PER_TOKEN;         // 3200

    public List<TextChunk> chunk(FileType type, List<ParsedSegment> segments) {
        return switch (type) {
            case PDF  -> pdf(segments);
            case PPTX -> pptx(segments);
            case DOCX -> docx(segments);
            case TXT  -> greedyPack(segments, "section");
            case MD   -> md(segments);
            case XLSX, CSV -> throw new IllegalArgumentException(
                type + " uses the spreadsheet path (§11.3), not the chunker");
        };
    }

    private int estTokens(String text) { return text.length() / CHARS_PER_TOKEN; }

    /** PDF: one page = one chunk if <=500 tokens; else split into parts, preserving order. */
    private List<TextChunk> pdf(List<ParsedSegment> segments) {
        List<TextChunk> out = new ArrayList<>();
        int ordinal = 0;
        for (ParsedSegment seg : segments) {
            String page = seg.metadata().getOrDefault("page", String.valueOf(seg.ordinal() + 1));
            if (estTokens(seg.text()) <= TARGET_TOKENS) {
                out.add(new TextChunk(ordinal++, seg.text(), Map.of("page", page)));
            } else {
                int part = 1;
                for (String piece : splitWithOverlap(seg.text())) {
                    Map<String, String> meta = new LinkedHashMap<>();
                    meta.put("page", page);
                    meta.put("part", String.valueOf(part++));
                    out.add(new TextChunk(ordinal++, piece, meta));
                }
            }
        }
        return out;
    }

    /** PPTX: one chunk per slide; split only if a slide body exceeds ~800 tokens. */
    private List<TextChunk> pptx(List<ParsedSegment> segments) {
        List<TextChunk> out = new ArrayList<>();
        int ordinal = 0;
        for (ParsedSegment seg : segments) {
            Map<String, String> base = new LinkedHashMap<>(seg.metadata());
            if (seg.text().length() <= PPTX_SPLIT_CHARS) {
                out.add(new TextChunk(ordinal++, seg.text(), base));
            } else {
                int part = 1;
                for (String piece : splitWithOverlap(seg.text())) {
                    Map<String, String> meta = new LinkedHashMap<>(base);
                    meta.put("part", String.valueOf(part++));
                    out.add(new TextChunk(ordinal++, piece, meta));
                }
            }
        }
        return out;
    }

    /** DOCX: concatenate paragraph segments until ~500 tokens, cut on paragraph boundary. */
    private List<TextChunk> docx(List<ParsedSegment> segments) {
        List<TextChunk> out = new ArrayList<>();
        int ordinal = 0;
        StringBuilder buf = new StringBuilder();
        int startPara = -1, lastPara = -1;
        for (ParsedSegment seg : segments) {
            int para = seg.ordinal();
            if (startPara < 0) startPara = para;
            if (buf.length() > 0) buf.append("\n\n");
            buf.append(seg.text());
            lastPara = para;
            if (estTokens(buf.toString()) >= TARGET_TOKENS) {
                out.add(new TextChunk(ordinal++, buf.toString(),
                    Map.of("paragraphRange", startPara + "-" + lastPara)));
                buf.setLength(0);
                startPara = -1;
            }
        }
        if (buf.length() > 0) {
            out.add(new TextChunk(ordinal++, buf.toString(),
                Map.of("paragraphRange", startPara + "-" + lastPara)));
        }
        return out;
    }

    /** MD: chunk on segment boundaries (parser emits per-heading sections); pack to ~500 tokens. */
    private List<TextChunk> md(List<ParsedSegment> segments) {
        List<TextChunk> out = new ArrayList<>();
        int ordinal = 0;
        for (ParsedSegment seg : segments) {
            String heading = seg.metadata().getOrDefault("heading", "");
            if (estTokens(seg.text()) <= TARGET_TOKENS) {
                out.add(new TextChunk(ordinal++, seg.text(),
                    heading.isEmpty() ? Map.of() : Map.of("heading", heading)));
            } else {
                int part = 1;
                for (String piece : splitWithOverlap(seg.text())) {
                    Map<String, String> meta = new LinkedHashMap<>();
                    if (!heading.isEmpty()) meta.put("heading", heading);
                    meta.put("part", String.valueOf(part++));
                    out.add(new TextChunk(ordinal++, piece, meta));
                }
            }
        }
        return out;
    }

    /** TXT: split on blank lines, greedy-pack paragraphs to ~500 tokens. */
    private List<TextChunk> greedyPack(List<ParsedSegment> segments, String metaKey) {
        String full = String.join("\n\n", segments.stream().map(ParsedSegment::text).toList());
        String[] paras = full.split("\\n\\s*\\n");
        List<TextChunk> out = new ArrayList<>();
        int ordinal = 0, section = 1;
        StringBuilder buf = new StringBuilder();
        for (String para : paras) {
            if (para.isBlank()) continue;
            if (buf.length() > 0 && estTokens(buf.toString()) + estTokens(para) > TARGET_TOKENS) {
                out.add(new TextChunk(ordinal++, buf.toString(), Map.of(metaKey, String.valueOf(section++))));
                buf.setLength(0);
            }
            if (buf.length() > 0) buf.append("\n\n");
            buf.append(para.strip());
        }
        if (buf.length() > 0) {
            out.add(new TextChunk(ordinal++, buf.toString(), Map.of(metaKey, String.valueOf(section))));
        }
        return out;
    }

    /** Split long text into ~500-token windows with 50-token overlap, on whitespace boundaries. */
    private List<String> splitWithOverlap(String text) {
        List<String> pieces = new ArrayList<>();
        int i = 0, n = text.length();
        while (i < n) {
            int end = Math.min(i + TARGET_CHARS, n);
            if (end < n) {
                int ws = text.lastIndexOf(' ', end);
                if (ws > i) end = ws;
            }
            pieces.add(text.substring(i, end).strip());
            if (end >= n) break;
            i = Math.max(end - OVERLAP_CHARS, i + 1);
        }
        return pieces;
    }
}
