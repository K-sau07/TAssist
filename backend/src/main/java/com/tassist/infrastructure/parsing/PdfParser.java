package com.tassist.infrastructure.parsing;

import com.tassist.domain.error.ValidationError;
import com.tassist.domain.port.out.DocumentParser;
import com.tassist.domain.vo.FileType;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.action.PDAction;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * PDF via PDFBox 3.x. One segment per page, tagged with {"page": n} provenance (§11.2).
 * <p>Extraction is position-sorted so tables and multi-column layouts read in visual order
 * instead of raw content-stream order (which scrambles table rows). Hyperlink URLs live in
 * page annotations, not the visible text, so they're pulled separately and appended per page —
 * otherwise a link like the User Manual URL would be lost to retrieval.
 */
@Component
public class PdfParser implements DocumentParser {

    @Override public FileType supportedType() { return FileType.PDF; }

    @Override public List<ParsedSegment> parse(byte[] content) {
        List<ParsedSegment> segments = new ArrayList<>();
        try (PDDocument doc = Loader.loadPDF(content)) {
            int pages = doc.getNumberOfPages();
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true); // reading order — fixes jumbled tables/columns

            for (int page = 1; page <= pages; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String text = stripper.getText(doc).strip();

                // Pull hyperlink URLs from this page's annotations and append them so RAG keeps them.
                String links = extractLinks(doc.getPage(page - 1));
                String combined = links.isEmpty() ? text : (text + "\n\nLinks on this page:\n" + links);

                if (!combined.isEmpty()) {
                    segments.add(new ParsedSegment(page - 1, combined,
                        Map.of("type", "pdf", "page", String.valueOf(page))));
                }
            }
        } catch (IOException e) {
            throw new ValidationError("could not parse PDF: " + e.getMessage());
        }
        return segments;
    }

    private static String extractLinks(PDPage page) {
        Set<String> urls = new LinkedHashSet<>();
        try {
            for (PDAnnotation ann : page.getAnnotations()) {
                if (ann instanceof PDAnnotationLink link) {
                    PDAction action = link.getAction();
                    if (action instanceof PDActionURI uri && uri.getURI() != null && !uri.getURI().isBlank()) {
                        urls.add(uri.getURI().strip());
                    }
                }
            }
        } catch (IOException ignored) {
            // annotation read failure shouldn't fail the whole parse
        }
        return String.join("\n", urls);
    }
}
