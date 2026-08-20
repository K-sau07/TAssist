package com.tassist.infrastructure.parsing;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import com.tassist.domain.error.ValidationError;
import com.tassist.domain.port.out.DocumentParser;
import com.tassist.domain.vo.FileType;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * CSV plain text-dump (D14 — Step 4). Each row becomes one segment "col=val, col=val".
 * Step 5 replaces this route with structured ingestion (§11.3).
 */
@Component
public class CsvParser implements DocumentParser {

    @Override public FileType supportedType() { return FileType.CSV; }

    @Override public List<ParsedSegment> parse(byte[] content) {
        List<ParsedSegment> segments = new ArrayList<>();
        try (CSVReader reader = new CSVReader(
                new InputStreamReader(new ByteArrayInputStream(content), StandardCharsets.UTF_8))) {
            String[] header = reader.readNext();
            if (header == null) return segments;
            String[] row;
            int rowNo = 0;
            while ((row = reader.readNext()) != null) {
                rowNo++;
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < row.length; i++) {
                    String col = i < header.length ? header[i] : ("col" + i);
                    if (i > 0) sb.append(", ");
                    sb.append(col).append("=").append(row[i]);
                }
                segments.add(new ParsedSegment(rowNo - 1, sb.toString(),
                    Map.of("type", "csv", "row", String.valueOf(rowNo))));
            }
        } catch (IOException | CsvValidationException e) {
            throw new ValidationError("could not parse CSV: " + e.getMessage());
        }
        return segments;
    }
}
