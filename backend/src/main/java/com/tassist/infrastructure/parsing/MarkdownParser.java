package com.tassist.infrastructure.parsing;

import com.tassist.domain.port.out.DocumentParser;
import com.tassist.domain.vo.FileType;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.text.TextContentRenderer;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Markdown: rendered to plain text (strips markup) so downstream chunking/embedding
 * sees prose, not syntax. One segment for the whole doc (§11.2).
 */
@Component
public class MarkdownParser implements DocumentParser {
    private final Parser parser = Parser.builder().build();
    private final TextContentRenderer renderer = TextContentRenderer.builder().build();

    @Override public FileType supportedType() { return FileType.MD; }

    @Override public List<ParsedSegment> parse(byte[] content) {
        String md = new String(content, StandardCharsets.UTF_8);
        Node doc = parser.parse(md);
        String text = renderer.render(doc);
        return List.of(new ParsedSegment(0, text, Map.of("type", "md")));
    }
}
