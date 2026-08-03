package com.tassist.domain.model;

import com.tassist.domain.vo.FileId;
import java.util.List;
import java.util.UUID;

/** Metadata for one sheet inside an XLSX/CSV, used for structured-mode retrieval (spec §8 / §11.3). */
public record SpreadsheetSheet(
        UUID id,
        FileId fileId,
        String sheetName,
        List<String> columnNames,
        List<String> columnTypes,
        long rowCount,
        String schemaSummary,
        float[] schemaSummaryEmbedding
) {
    public SpreadsheetSheet {
        if (id == null) throw new IllegalArgumentException("SpreadsheetSheet.id must not be null");
        if (fileId == null) throw new IllegalArgumentException("SpreadsheetSheet.fileId must not be null");
        if (sheetName == null || sheetName.isBlank())
            throw new IllegalArgumentException("SpreadsheetSheet.sheetName must not be blank");
        columnNames = columnNames == null ? List.of() : List.copyOf(columnNames);
        columnTypes = columnTypes == null ? List.of() : List.copyOf(columnTypes);
        if (rowCount < 0) throw new IllegalArgumentException("SpreadsheetSheet.rowCount must not be negative");
        if (schemaSummary == null) throw new IllegalArgumentException("SpreadsheetSheet.schemaSummary must not be null");
    }
}
