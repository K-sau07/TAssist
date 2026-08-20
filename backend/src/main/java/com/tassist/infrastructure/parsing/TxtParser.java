package com.tassist.infrastructure.parsing;

import com.tassist.domain.port.out.DocumentParser;
import com.tassist.domain.vo.FileType;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/** Plain text: one segment carrying the whole document (§11.2). */
@Component
public class TxtParser implements DocumentParser {
    @Override public FileType supportedType() { return FileType.TXT; }

    @Override public List<ParsedSegment> parse(byte[] content) {
        String text = new String(content, StandardCharsets.UTF_8);
        return List.of(new ParsedSegment(0, text, Map.of("type", "txt")));
    }
}
