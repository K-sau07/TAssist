package com.tassist.application.ingest;

import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.List;

/**
 * Infers a column's type by sampling values (§11.3 step 2: sample first 100 non-empty rows).
 * Rules: a column is NUMBER/BOOLEAN/DATE only if EVERY sampled non-empty value parses as that
 * type; otherwise it falls back to TEXT. Empty samples default to TEXT.
 */
public final class ColumnTypeInferrer {

    public static final int SAMPLE_SIZE = 100;

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
        DateTimeFormatter.ISO_LOCAL_DATE,               // 2024-01-05
        DateTimeFormatter.ofPattern("yyyy/MM/dd"),
        DateTimeFormatter.ofPattern("MM/dd/yyyy"),
        DateTimeFormatter.ofPattern("dd-MM-yyyy"),
        DateTimeFormatter.ISO_LOCAL_DATE_TIME           // 2024-01-05T10:15:30
    );

    private ColumnTypeInferrer() {}

    /** samples: raw string values already filtered to non-null; blanks are skipped internally. */
    public static ColumnType infer(List<String> samples) {
        int seen = 0;
        boolean allNumber = true, allBoolean = true, allDate = true;
        for (String raw : samples) {
            if (raw == null) continue;
            String v = raw.trim();
            if (v.isEmpty()) continue;
            if (seen >= SAMPLE_SIZE) break;
            seen++;
            if (allNumber && !isNumber(v)) allNumber = false;
            if (allBoolean && !isBoolean(v)) allBoolean = false;
            if (allDate && !isDate(v)) allDate = false;
        }
        if (seen == 0) return ColumnType.TEXT;
        // Precedence: BOOLEAN before NUMBER (0/1 would parse as number too), DATE, else NUMBER, else TEXT.
        if (allBoolean) return ColumnType.BOOLEAN;
        if (allNumber) return ColumnType.NUMBER;
        if (allDate) return ColumnType.DATE;
        return ColumnType.TEXT;
    }

    static boolean isNumber(String v) {
        try { Double.parseDouble(v.replace(",", "")); return true; }
        catch (NumberFormatException e) { return false; }
    }

    static boolean isBoolean(String v) {
        String l = v.toLowerCase();
        return l.equals("true") || l.equals("false") || l.equals("yes") || l.equals("no");
    }

    static boolean isDate(String v) {
        for (DateTimeFormatter f : DATE_FORMATS) {
            try { TemporalAccessor ignored = f.parse(v); return true; }
            catch (Exception ignored) { /* try next */ }
        }
        return false;
    }
}
