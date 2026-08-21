package com.tassist.application.generation;

import com.tassist.domain.vo.FileType;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

/** Unit tests for §11.8 private-library citation labels. */
class CitationLabelerTest {

    @Test void pdf_page() {
        assertThat(CitationLabeler.label("lecture-04.pdf", FileType.PDF, Map.of("page", "7")))
            .isEqualTo("lecture-04.pdf, page 7");
    }
    @Test void pdf_page_with_part() {
        assertThat(CitationLabeler.label("big.pdf", FileType.PDF, Map.of("page", "3", "part", "2")))
            .isEqualTo("big.pdf, page 3 (part 2)");
    }
    @Test void docx_paragraph_range() {
        assertThat(CitationLabeler.label("onboarding.docx", FileType.DOCX, Map.of("paragraphRange", "12")))
            .isEqualTo("onboarding.docx, \u00b612");
    }
    @Test void pptx_slide_with_title() {
        assertThat(CitationLabeler.label("slides.pptx", FileType.PPTX, Map.of("slide", "3", "title", "Recursion")))
            .isEqualTo("slides.pptx, slide 3 \u2014 'Recursion'");
    }
    @Test void md_heading() {
        assertThat(CitationLabeler.label("readme.md", FileType.MD, Map.of("heading", "Setup")))
            .isEqualTo("readme.md, \u00a7 Setup");
    }
    @Test void no_metadata_just_filename() {
        assertThat(CitationLabeler.label("plain.txt", FileType.TXT, Map.of()))
            .isEqualTo("plain.txt");
    }
}
