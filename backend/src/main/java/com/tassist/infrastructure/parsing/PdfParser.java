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
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * PDF via PDFBox 3.x. One segment per page, tagged with {"page": n} provenance (§11.2).
 * <p>Three-layer extraction so nothing is lost:
 * <ol>
 *   <li>Text layer via {@link PDFTextStripper} (position-sorted → correct table/column order).</li>
 *   <li>Hyperlink URLs from page annotations (invisible to the text layer).</li>
 *   <li>OCR over the rendered page when the text layer is thin (image-based pages: screenshots
 *       of tables/code). Degrades gracefully if OCR is unavailable.</li>
 * </ol>
 */
@Component
public class PdfParser implements DocumentParser {
    private static final Logger log = LoggerFactory.getLogger(PdfParser.class);

    private final OcrService ocr;
    private final int ocrDpi;
    private final int minTextChars;

    public PdfParser(OcrService ocr,
                     @Value("${tassist.ocr.dpi:200}") int ocrDpi,
                     @Value("${tassist.ocr.min-text-chars:80}") int minTextChars) {
        this.ocr = ocr;
        this.ocrDpi = ocrDpi;
        this.minTextChars = minTextChars;
    }

    @Override public FileType supportedType() { return FileType.PDF; }

    @Override public List<ParsedSegment> parse(byte[] content) {
        List<ParsedSegment> segments = new ArrayList<>();
        try (PDDocument doc = Loader.loadPDF(content)) {
            int pages = doc.getNumberOfPages();
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            PDFRenderer renderer = ocr.isEnabled() ? new PDFRenderer(doc) : null;

            for (int page = 1; page <= pages; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String text = stripper.getText(doc).strip();

                // OCR any page that carries images — image-embedded text (screenshots of tables/code)
                // is complementary to the text layer, so we always add it when images are present.
                // De-duplication below prevents doubling text that OCR re-reads from the text layer.
                String ocrText = "";
                if (renderer != null && pageHasImages(doc.getPage(page - 1))) {
                    ocrText = dedupeAgainst(ocrPage(renderer, page - 1), text);
                }

                String links = extractLinks(doc.getPage(page - 1));

                StringBuilder combined = new StringBuilder(text);
                if (!ocrText.isBlank()) {
                    if (combined.length() > 0) combined.append("\n\n");
                    combined.append(ocrText);
                }
                if (!links.isEmpty()) combined.append("\n\nLinks on this page:\n").append(links);

                String finalText = combined.toString().strip();
                if (!finalText.isEmpty()) {
                    segments.add(new ParsedSegment(page - 1, finalText,
                        Map.of("type", "pdf", "page", String.valueOf(page))));
                }
            }
        } catch (IOException e) {
            throw new ValidationError("could not parse PDF: " + e.getMessage());
        }
        return segments;
    }

    private String ocrPage(PDFRenderer renderer, int pageIndex) {
        try {
            BufferedImage img = renderer.renderImageWithDPI(pageIndex, ocrDpi, ImageType.GRAY);
            String out = ocr.ocr(img);
            if (!out.isBlank()) log.debug("OCR recovered {} chars from page {}", out.length(), pageIndex + 1);
            return out;
        } catch (Throwable e) {
            log.warn("OCR render failed on page {}: {}", pageIndex + 1, e.getMessage());
            return "";
        }
    }

    private static boolean pageHasImages(PDPage page) {
        try {
            var res = page.getResources();
            if (res == null) return false;
            for (var name : res.getXObjectNames()) {
                var xo = res.getXObject(name);
                if (xo instanceof org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject) return true;
            }
        } catch (IOException ignored) { }
        return false;
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
        } catch (IOException ignored) { }
        return String.join("\n", urls);
    }

    /** Keep only OCR lines not already in the text layer (avoids duplicating text OCR re-reads). */
    private static String dedupeAgainst(String ocrText, String textLayer) {
        if (ocrText == null || ocrText.isBlank()) return "";
        String hay = textLayer == null ? "" : textLayer.toLowerCase();
        StringBuilder kept = new StringBuilder();
        for (String line : ocrText.split("\r?\n")) {
            String t = line.strip();
            if (t.length() < 4) continue;                       // drop noise
            if (hay.contains(t.toLowerCase())) continue;        // already in text layer
            kept.append(t).append("\n");
        }
        return kept.toString().strip();
    }
}
