package com.tassist.domain.model;

import java.util.Map;
import java.util.UUID;

/** One data row of a spreadsheet sheet, stored for structured querying (spec §8). */
public record SpreadsheetRow(
        UUID id,
        UUID sheetId,
        long rowNumber,
        Map<String, Object> values
) {
    public SpreadsheetRow {
        if (id == null) throw new IllegalArgumentException("SpreadsheetRow.id must not be null");
        if (sheetId == null) throw new IllegalArgumentException("SpreadsheetRow.sheetId must not be null");
        if (rowNumber < 1) throw new IllegalArgumentException("SpreadsheetRow.rowNumber is 1-indexed (>= 1)");
        values = values == null ? Map.of() : Map.copyOf(values);
    }
}
