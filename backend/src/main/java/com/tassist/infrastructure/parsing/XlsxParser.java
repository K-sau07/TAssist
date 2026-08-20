package com.tassist.infrastructure.parsing;

import com.tassist.domain.error.ValidationError;
import com.tassist.domain.port.out.DocumentParser;
import com.tassist.domain.vo.FileType;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * XLSX plain text-dump (D14 — Step 4). One segment per row per sheet, "cells joined by tab".
 * Step 5 replaces this route with structured ingestion (§11.3).
 */
@Component
public class XlsxParser implements DocumentParser {

    private final DataFormatter fmt = new DataFormatter();

    @Override public FileType supportedType() { return FileType.XLSX; }

    @Override public List<ParsedSegment> parse(byte[] content) {
        List<ParsedSegment> segments = new ArrayList<>();
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(content))) {
            int ordinal = 0;
            for (int s = 0; s < wb.getNumberOfSheets(); s++) {
                Sheet sheet = wb.getSheetAt(s);
                String sheetName = sheet.getSheetName();
                for (Row row : sheet) {
                    StringBuilder sb = new StringBuilder();
                    for (Cell cell : row) {
                        if (sb.length() > 0) sb.append("\t");
                        sb.append(fmt.formatCellValue(cell));
                    }
                    String text = sb.toString().strip();
                    if (!text.isEmpty()) {
                        segments.add(new ParsedSegment(ordinal++, text,
                            Map.of("type", "xlsx", "sheet", sheetName,
                                   "row", String.valueOf(row.getRowNum() + 1))));
                    }
                }
            }
        } catch (IOException e) {
            throw new ValidationError("could not parse XLSX: " + e.getMessage());
        }
        return segments;
    }
}
