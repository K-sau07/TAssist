package com.tassist.application.ingest;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

/** Unit tests for §11.3 step-5 schema summary text. */
class SchemaSummarizerTest {

    private final SchemaSummarizer summarizer = new SchemaSummarizer();

    @Test void summary_has_counts_columns_types_and_samples() {
        ParsedSheet sheet = new ParsedSheet(
            "Sales",
            List.of("product", "units", "revenue"),
            List.of(ColumnType.TEXT, ColumnType.NUMBER, ColumnType.NUMBER),
            List.of(
                Map.of("product", "Widget A", "units", "10", "revenue", "100.5"),
                Map.of("product", "Widget B", "units", "20", "revenue", "200.0")));
        String s = summarizer.summarize(sheet);
        assertThat(s).startsWith("Sheet 'Sales' has 2 rows and 3 columns:");
        assertThat(s).contains("product (TEXT)").contains("units (NUMBER)").contains("revenue (NUMBER)");
        assertThat(s).contains("Sample values —");
        assertThat(s).contains("product: 'Widget A'");   // TEXT quoted
        assertThat(s).contains("units: 10");             // NUMBER unquoted
    }

    @Test void singular_column_wording() {
        ParsedSheet sheet = new ParsedSheet("S", List.of("only"), List.of(ColumnType.TEXT),
            List.of(Map.of("only", "x")));
        assertThat(summarizer.summarize(sheet)).contains("1 column:");
    }

    @Test void empty_rows_omit_sample_clause() {
        ParsedSheet sheet = new ParsedSheet("Empty", List.of("a"), List.of(ColumnType.TEXT), List.of());
        String s = summarizer.summarize(sheet);
        assertThat(s).contains("0 rows");
        assertThat(s).doesNotContain("Sample values");
    }
}
