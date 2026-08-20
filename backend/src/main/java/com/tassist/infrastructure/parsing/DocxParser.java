package com.tassist.infrastructure.parsing;

import com.tassist.domain.error.ValidationError;
import com.tassist.domain.port.out.DocumentParser;
import com.tassist.domain.vo.FileType;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * DOCX via POI. Emits paragraph text as ordered segments (§11.2). One segment per
 * non-empty paragraph keeps provenance fine-grained; the chunker merges later (Step 5).
 */
@Component
public class DocxParser implements DocumentParser {

    @Override public FileType supportedType() { return FileType.DOCX; }

    @Override public List<ParsedSegment> parse(byte[] content) {
        List<ParsedSegment> segments = new ArrayList<>();
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(content))) {
            int ordinal = 0;
            for (XWPFParagraph p : doc.getParagraphs()) {
                String text = p.getText() == null ? "" : p.getText().strip();
                if (!text.isEmpty()) {
                    segments.add(new ParsedSegment(ordinal++, text,
                        Map.of("type", "docx", "paragraph", String.valueOf(ordinal))));
                }
            }
        } catch (IOException e) {
            throw new ValidationError("could not parse DOCX: " + e.getMessage());
        }
        return segments;
    }
}
