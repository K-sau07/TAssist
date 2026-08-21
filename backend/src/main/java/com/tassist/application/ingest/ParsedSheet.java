package com.tassist.application.ingest;

import java.util.List;
import java.util.Map;

/**
 * One parsed sheet from an XLSX/CSV, prior to persistence (§11.3 steps 1-4).
 * rows are 1-indexed logically by position in the list (first data row = rowNumber 1);
 * each row maps declared column name -> cell value (String, Double, Boolean, or null).
 */
public record ParsedSheet(
        String sheetName,
        List<String> columnNames,
        List<ColumnType> columnTypes,
        List<Map<String, Object>> rows
) {
    public ParsedSheet {
        if (sheetName == null || sheetName.isBlank())
            throw new IllegalArgumentException("ParsedSheet.sheetName must not be blank");
        columnNames = columnNames == null ? List.of() : List.copyOf(columnNames);
        columnTypes = columnTypes == null ? List.of() : List.copyOf(columnTypes);
        if (columnNames.size() != columnTypes.size())
            throw new IllegalArgumentException("column names/types size mismatch: "
                + columnNames.size() + " vs " + columnTypes.size());
        rows = rows == null ? List.of() : List.copyOf(rows);
    }

    public long rowCount() { return rows.size(); }
}
