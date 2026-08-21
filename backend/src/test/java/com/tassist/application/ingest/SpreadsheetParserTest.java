package com.tassist.application.ingest;

import com.tassist.domain.vo.FileType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Unit tests for §11.3 structured parsing (XLSX via POI, CSV via OpenCSV). */
class SpreadsheetParserTest {

    private final SpreadsheetParser parser = new SpreadsheetParser();

    private static byte[] xlsx(String[] header, Object[][] data) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Sales");
            Row h = sheet.createRow(0);
            for (int c = 0; c < header.length; c++) h.createCell(c).setCellValue(header[c]);
            for (int r = 0; r < data.length; r++) {
                Row row = sheet.createRow(r + 1);
                for (int c = 0; c < data[r].length; c++) {
                    Object v = data[r][c];
                    if (v instanceof Number n) row.createCell(c).setCellValue(n.doubleValue());
                    else row.createCell(c).setCellValue(String.valueOf(v));
                }
            }
            wb.write(out);
            return out.toByteArray();
        }
    }

    @Test void xlsx_headers_types_and_rows() throws Exception {
        byte[] bytes = xlsx(
            new String[]{"product", "region", "units", "revenue"},
            new Object[][]{
                {"Widget A", "APAC", 10, 100.5},
                {"Widget B", "EMEA", 20, 200.0},
            });
        List<ParsedSheet> sheets = parser.parse(FileType.XLSX, bytes);
        assertThat(sheets).hasSize(1);
        ParsedSheet s = sheets.get(0);
        assertThat(s.sheetName()).isEqualTo("Sales");
        assertThat(s.columnNames()).containsExactly("product", "region", "units", "revenue");
        assertThat(s.columnTypes()).containsExactly(
            ColumnType.TEXT, ColumnType.TEXT, ColumnType.NUMBER, ColumnType.NUMBER);
        assertThat(s.rowCount()).isEqualTo(2);
        assertThat(s.rows().get(0)).containsEntry("product", "Widget A").containsEntry("region", "APAC");
    }

    @Test void csv_single_sheet_parsed() {
        String csv = "date,product,active\n2024-01-05,Widget A,true\n2024-02-01,Widget B,false\n";
        List<ParsedSheet> sheets = parser.parse(FileType.CSV, csv.getBytes(StandardCharsets.UTF_8));
        assertThat(sheets).hasSize(1);
        ParsedSheet s = sheets.get(0);
        assertThat(s.columnNames()).containsExactly("date", "product", "active");
        assertThat(s.columnTypes()).containsExactly(ColumnType.DATE, ColumnType.TEXT, ColumnType.BOOLEAN);
        assertThat(s.rowCount()).isEqualTo(2);
    }

    @Test void blank_header_cells_get_synthetic_names() {
        String csv = "name,,value\nAlice,x,10\n";
        ParsedSheet s = parser.parse(FileType.CSV, csv.getBytes(StandardCharsets.UTF_8)).get(0);
        assertThat(s.columnNames()).containsExactly("name", "column_2", "value");
    }

    @Test void missing_trailing_cells_become_null() {
        String csv = "a,b,c\n1,2\n";
        ParsedSheet s = parser.parse(FileType.CSV, csv.getBytes(StandardCharsets.UTF_8)).get(0);
        Map<String, Object> row = s.rows().get(0);
        assertThat(row.get("c")).isNull();
    }
}
