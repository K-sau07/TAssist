package com.tassist.application.ingest;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import com.tassist.domain.error.ValidationError;
import com.tassist.domain.vo.FileType;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Structured spreadsheet parser (§11.3 steps 1-2). Produces one ParsedSheet per source sheet
 * (CSV = single sheet). Row 1 is treated as the header. Column types are inferred by sampling.
 * This is the structured route that REPLACES the Step-4 plain-dump for XLSX/CSV (D14/D16).
 */
@Component
public class SpreadsheetParser {

    private final DataFormatter fmt = new DataFormatter();

    public List<ParsedSheet> parse(FileType type, byte[] content) {
        return switch (type) {
            case XLSX -> parseXlsx(content);
            case CSV -> List.of(parseCsv(content));
            default -> throw new ValidationError("SpreadsheetParser does not support type " + type);
        };
    }

    // ---- XLSX (POI XSSF) ----
    private List<ParsedSheet> parseXlsx(byte[] content) {
        List<ParsedSheet> out = new ArrayList<>();
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(content))) {
            for (int s = 0; s < wb.getNumberOfSheets(); s++) {
                Sheet sheet = wb.getSheetAt(s);
                List<List<String>> grid = new ArrayList<>();
                int maxCols = 0;
                for (Row row : sheet) {
                    int last = row.getLastCellNum();
                    if (last < 0) continue;
                    maxCols = Math.max(maxCols, last);
                    List<String> cells = new ArrayList<>(last);
                    for (int c = 0; c < last; c++) {
                        Cell cell = row.getCell(c);
                        cells.add(cell == null ? "" : cellToString(cell));
                    }
                    grid.add(cells);
                }
                if (grid.isEmpty()) continue;
                out.add(buildSheet(sheet.getSheetName(), grid, maxCols));
            }
        } catch (IOException e) {
            throw new ValidationError("failed to read XLSX: " + e.getMessage());
        }
        if (out.isEmpty()) throw new ValidationError("XLSX has no readable sheets");
        return out;
    }

    private String cellToString(Cell cell) {
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate().toString();
        }
        return fmt.formatCellValue(cell);
    }

    // ---- CSV (OpenCSV) ----
    private ParsedSheet parseCsv(byte[] content) {
        List<List<String>> grid = new ArrayList<>();
        int maxCols = 0;
        try (CSVReader reader = new CSVReader(
                new InputStreamReader(new ByteArrayInputStream(content), StandardCharsets.UTF_8))) {
            String[] line;
            while ((line = reader.readNext()) != null) {
                maxCols = Math.max(maxCols, line.length);
                grid.add(List.of(line));
            }
        } catch (IOException | CsvValidationException e) {
            throw new ValidationError("failed to read CSV: " + e.getMessage());
        }
        if (grid.isEmpty()) throw new ValidationError("CSV is empty");
        return buildSheet("Sheet1", grid, maxCols);
    }

    // ---- shared: header detection, type inference, row maps ----
    private ParsedSheet buildSheet(String name, List<List<String>> grid, int maxCols) {
        List<String> header = normalizeHeader(grid.get(0), maxCols);
        List<Map<String, Object>> rows = new ArrayList<>(Math.max(0, grid.size() - 1));
        for (int r = 1; r < grid.size(); r++) {
            List<String> raw = grid.get(r);
            Map<String, Object> values = new LinkedHashMap<>();
            for (int c = 0; c < header.size(); c++) {
                String cell = c < raw.size() ? raw.get(c) : "";
                values.put(header.get(c), cell == null || cell.isEmpty() ? null : cell);
            }
            rows.add(values);
        }
        List<ColumnType> types = inferTypes(header, rows);
        return new ParsedSheet(name, header, types, rows);
    }

    private List<String> normalizeHeader(List<String> firstRow, int maxCols) {
        List<String> header = new ArrayList<>(maxCols);
        for (int c = 0; c < maxCols; c++) {
            String h = c < firstRow.size() ? firstRow.get(c) : "";
            if (h == null || h.trim().isEmpty()) h = "column_" + (c + 1);
            else h = h.trim();
            // de-duplicate collisions
            String base = h; int n = 1;
            while (header.contains(h)) h = base + "_" + (++n);
            header.add(h);
        }
        return header;
    }

    private List<ColumnType> inferTypes(List<String> header, List<Map<String, Object>> rows) {
        List<ColumnType> types = new ArrayList<>(header.size());
        for (String col : header) {
            List<String> samples = new ArrayList<>(ColumnTypeInferrer.SAMPLE_SIZE);
            for (Map<String, Object> row : rows) {
                Object v = row.get(col);
                if (v != null) samples.add(v.toString());
                if (samples.size() >= ColumnTypeInferrer.SAMPLE_SIZE) break;
            }
            types.add(ColumnTypeInferrer.infer(samples));
        }
        return types;
    }
}
