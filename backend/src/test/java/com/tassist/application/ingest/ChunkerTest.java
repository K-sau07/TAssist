package com.tassist.application.ingest;

import com.tassist.domain.port.out.DocumentParser.ParsedSegment;
import com.tassist.domain.vo.FileType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Unit tests for §11.2 chunking rules. Pure logic, no Spring. */
class ChunkerTest {

    private final Chunker chunker = new Chunker();

    private static String words(int n) {
        return "word ".repeat(n).strip();
    }

    @Test
    void pdf_short_page_is_one_chunk_with_page_metadata() {
        var segs = List.of(new ParsedSegment(0, "a short page", Map.of("page", "1")));
        List<TextChunk> chunks = chunker.chunk(FileType.PDF, segs);
        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).metadata()).containsEntry("page", "1");
        assertThat(chunks.get(0).ordinal()).isZero();
    }

    @Test
    void pdf_long_page_splits_into_parts() {
        // > 500 tokens => > 2000 chars. 3000 words ~ 15000 chars.
        var segs = List.of(new ParsedSegment(0, words(3000), Map.of("page", "7")));
        List<TextChunk> chunks = chunker.chunk(FileType.PDF, segs);
        assertThat(chunks.size()).isGreaterThan(1);
        assertThat(chunks).allSatisfy(c -> assertThat(c.metadata()).containsEntry("page", "7"));
        assertThat(chunks.get(0).metadata()).containsEntry("part", "1");
        // ordinals are contiguous from 0
        for (int i = 0; i < chunks.size(); i++) assertThat(chunks.get(i).ordinal()).isEqualTo(i);
    }

    @Test
    void docx_packs_paragraphs_and_records_range() {
        // each ~150 tokens (600 chars); 5 paras => should pack into ~2 chunks
        var segs = List.of(
            new ParsedSegment(0, words(150), Map.of()),
            new ParsedSegment(1, words(150), Map.of()),
            new ParsedSegment(2, words(150), Map.of()),
            new ParsedSegment(3, words(150), Map.of()),
            new ParsedSegment(4, words(150), Map.of()));
        List<TextChunk> chunks = chunker.chunk(FileType.DOCX, segs);
        assertThat(chunks).isNotEmpty();
        assertThat(chunks.get(0).metadata()).containsKey("paragraphRange");
    }

    @Test
    void pptx_one_chunk_per_slide() {
        var segs = List.of(
            new ParsedSegment(0, "[Slide 1 — Intro]\n\nbody", Map.of("slide", "1", "title", "Intro")),
            new ParsedSegment(1, "[Slide 2 — Next]\n\nbody", Map.of("slide", "2", "title", "Next")));
        List<TextChunk> chunks = chunker.chunk(FileType.PPTX, segs);
        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0).metadata()).containsEntry("slide", "1").containsEntry("title", "Intro");
    }

    @Test
    void txt_greedy_packs_paragraphs() {
        String big = words(150) + "\n\n" + words(150) + "\n\n" + words(150) + "\n\n"
                   + words(150) + "\n\n" + words(150);
        var segs = List.of(new ParsedSegment(0, big, Map.of()));
        List<TextChunk> chunks = chunker.chunk(FileType.TXT, segs);
        assertThat(chunks).isNotEmpty();
        assertThat(chunks.get(0).metadata()).containsKey("section");
    }

    @Test
    void md_carries_heading_metadata() {
        var segs = List.of(new ParsedSegment(0, "# Title\n\nsome content here", Map.of("heading", "Title")));
        List<TextChunk> chunks = chunker.chunk(FileType.MD, segs);
        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).metadata()).containsEntry("heading", "Title");
    }

    @Test
    void spreadsheets_are_rejected_by_the_chunker() {
        var segs = List.of(new ParsedSegment(0, "a,b,c", Map.of()));
        assertThatThrownBy(() -> chunker.chunk(FileType.XLSX, segs))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> chunker.chunk(FileType.CSV, segs))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void ordinals_are_contiguous_across_multi_segment_input() {
        var segs = List.of(
            new ParsedSegment(0, "page one", Map.of("page", "1")),
            new ParsedSegment(1, "page two", Map.of("page", "2")),
            new ParsedSegment(2, "page three", Map.of("page", "3")));
        List<TextChunk> chunks = chunker.chunk(FileType.PDF, segs);
        for (int i = 0; i < chunks.size(); i++) assertThat(chunks.get(i).ordinal()).isEqualTo(i);
    }
}
