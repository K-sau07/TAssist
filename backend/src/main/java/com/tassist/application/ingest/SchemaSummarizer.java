package com.tassist.application.ingest;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Builds the natural-language schema summary embedded for spreadsheet retrieval (§11.3 step 5).
 * Format: "Sheet 'X' has N rows and M columns: col (TYPE), ... . Sample values — col: v, ..."
 */
@Component
public class SchemaSummarizer {

    private static final int SAMPLE_COLS = 6; // cap sample-value clause width

    public String summarize(ParsedSheet sheet) {
        List<String> cols = sheet.columnNames();
        List<ColumnType> types = sheet.columnTypes();

        StringBuilder sb = new StringBuilder();
        sb.append("Sheet '").append(sheet.sheetName()).append("' has ")
          .append(sheet.rowCount()).append(" rows and ")
          .append(cols.size()).append(cols.size() == 1 ? " column: " : " columns: ");

        StringJoiner colJoiner = new StringJoiner(", ");
        for (int i = 0; i < cols.size(); i++) {
            colJoiner.add(cols.get(i) + " (" + types.get(i) + ")");
        }
        sb.append(colJoiner).append(".");

        String samples = sampleClause(sheet);
        if (!samples.isEmpty()) sb.append(" Sample values — ").append(samples).append(".");
        return sb.toString();
    }

    /** First non-null value per column, for up to SAMPLE_COLS columns. */
    private String sampleClause(ParsedSheet sheet) {
        if (sheet.rows().isEmpty()) return "";
        List<String> cols = sheet.columnNames();
        List<ColumnType> types = sheet.columnTypes();
        StringJoiner sj = new StringJoiner(", ");
        int shown = 0;
        for (int i = 0; i < cols.size() && shown < SAMPLE_COLS; i++) {
            String col = cols.get(i);
            Object sample = firstNonNull(sheet.rows(), col);
            if (sample == null) continue;
            String rendered = types.get(i) == ColumnType.TEXT ? "'" + sample + "'" : sample.toString();
            sj.add(col + ": " + rendered);
            shown++;
        }
        return sj.toString();
    }

    private Object firstNonNull(List<Map<String, Object>> rows, String col) {
        for (Map<String, Object> row : rows) {
            Object v = row.get(col);
            if (v != null) return v;
        }
        return null;
    }
}
