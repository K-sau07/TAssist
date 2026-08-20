package com.tassist.infrastructure.parsing;

import com.tassist.domain.error.ValidationError;
import com.tassist.domain.port.out.DocumentParser;
import com.tassist.domain.vo.FileType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Resolves the DocumentParser for a given FileType. Collects all parser beans at startup. */
@Component
public class ParserRegistry {

    private final Map<FileType, DocumentParser> byType;

    public ParserRegistry(List<DocumentParser> parsers) {
        this.byType = parsers.stream()
            .collect(Collectors.toMap(DocumentParser::supportedType, Function.identity()));
    }

    public DocumentParser forType(FileType type) {
        DocumentParser p = byType.get(type);
        if (p == null) throw new ValidationError("no parser for file type " + type);
        return p;
    }
}
