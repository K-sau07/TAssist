package com.tassist.application.ingest;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

/** Unit tests for §11.3 step-2 column type inference. */
class ColumnTypeInferrerTest {

    @Test void all_integers_are_number() {
        assertThat(ColumnTypeInferrer.infer(List.of("1", "2", "3", "1000"))).isEqualTo(ColumnType.NUMBER);
    }

    @Test void decimals_and_thousands_are_number() {
        assertThat(ColumnTypeInferrer.infer(List.of("1,200.50", "3.14", "42"))).isEqualTo(ColumnType.NUMBER);
    }

    @Test void true_false_yes_no_are_boolean() {
        assertThat(ColumnTypeInferrer.infer(List.of("true", "false", "yes", "no"))).isEqualTo(ColumnType.BOOLEAN);
    }

    @Test void iso_dates_are_date() {
        assertThat(ColumnTypeInferrer.infer(List.of("2024-01-05", "2023-12-31"))).isEqualTo(ColumnType.DATE);
    }

    @Test void mixed_falls_back_to_text() {
        assertThat(ColumnTypeInferrer.infer(List.of("1", "hello", "2024-01-05"))).isEqualTo(ColumnType.TEXT);
    }

    @Test void empty_or_blank_only_defaults_to_text() {
        assertThat(ColumnTypeInferrer.infer(List.of("", "  ", ""))).isEqualTo(ColumnType.TEXT);
    }

    @Test void blanks_are_skipped_not_counted_against_type() {
        assertThat(ColumnTypeInferrer.infer(List.of("1", "", "2", "  ", "3"))).isEqualTo(ColumnType.NUMBER);
    }
}
