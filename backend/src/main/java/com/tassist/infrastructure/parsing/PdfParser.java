package com.tassist.infrastructure.parsing;

import com.tassist.domain.error.ValidationError;
import com.tassist.domain.port.out.DocumentParser;
import com.tassist.domain.vo.FileType;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** PDF via PDFBox 3.x. One segment per page, tagged with {"page": n} provenance (§11.2). */
@Component
public class PdfParser implements DocumentParser {

    @Override public FileType supportedType() { return FileType.PDF; }

    @Override public List<ParsedSegment> parse(byte[] content) {
        List<ParsedSegment> segments = new ArrayList<>();
        try (PDDocument doc = Loader.loadPDF(content)) {
            int pages = doc.getNumberOfPages();
            PDFTextStripper stripper = new PDFTextStripper();
            for (int page = 1; page <= pages; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String text = stripper.getText(doc).strip();
                if (!text.isEmpty()) {
                    segments.add(new ParsedSegment(page - 1, text,
                        Map.of("type", "pdf", "page", String.valueOf(page))));
                }
            }
        } catch (IOException e) {
            throw new ValidationError("could not parse PDF: " + e.getMessage());
        }
        return segments;
    }
}
