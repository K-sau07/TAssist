package com.tassist.infrastructure.parsing;

import com.tassist.domain.error.ValidationError;
import com.tassist.domain.port.out.DocumentParser;
import com.tassist.domain.vo.FileType;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** PPTX via POI. One segment per slide; concatenates all text shapes on the slide (§11.2). */
@Component
public class PptxParser implements DocumentParser {

    @Override public FileType supportedType() { return FileType.PPTX; }

    @Override public List<ParsedSegment> parse(byte[] content) {
        List<ParsedSegment> segments = new ArrayList<>();
        try (XMLSlideShow ppt = new XMLSlideShow(new ByteArrayInputStream(content))) {
            int slideNo = 0;
            for (XSLFSlide slide : ppt.getSlides()) {
                slideNo++;
                StringBuilder sb = new StringBuilder();
                for (XSLFShape shape : slide.getShapes()) {
                    if (shape instanceof XSLFTextShape ts) {
                        String t = ts.getText();
                        if (t != null && !t.isBlank()) sb.append(t.strip()).append("\n");
                    }
                }
                String text = sb.toString().strip();
                if (!text.isEmpty()) {
                    segments.add(new ParsedSegment(slideNo - 1, text,
                        Map.of("type", "pptx", "slide", String.valueOf(slideNo))));
                }
            }
        } catch (IOException e) {
            throw new ValidationError("could not parse PPTX: " + e.getMessage());
        }
        return segments;
    }
}
