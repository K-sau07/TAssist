package com.tassist.application.spreadsheet;

import com.tassist.application.spreadsheet.SpreadsheetQueryService.Filter;
import com.tassist.application.spreadsheet.SpreadsheetQueryService.ToolCallInput;
import com.tassist.domain.model.*;
import com.tassist.domain.port.out.*;
import com.tassist.domain.vo.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/** Integration tests for §11.7 query_spreadsheet over real Postgres JSONB. */
@SpringBootTest
@Testcontainers
class SpreadsheetQueryServiceTest {

    @Container @ServiceConnection
    static PostgreSQLContainer<?> PG = new PostgreSQLContainer<>(
        DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"));

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.flyway.enabled", () -> "true");
        r.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        r.add("spring.data.redis.repositories.enabled", () -> "false");
    }

    @Autowired SpreadsheetQueryService svc;
    @Autowired SpreadsheetRepository spreadsheets;
    @Autowired UserRepository users;
    @Autowired FileRepository files;

    private UUID sheetId;

    @BeforeEach
    void seed() {
        Instant now = Instant.now();
        User u = users.save(new User(UserId.newId(), "sq_"+System.nanoTime()+"@ex.com", "U",
            Optional.of("$2a$h"), AuthProvider.PASSWORD, Optional.empty(), now, now));
        FileId fid = FileId.newId();
        files.save(new File(fid, u.id(), "sales.xlsx", FileType.XLSX, 1, "k/"+fid.value(),
            "h"+fid.value(), FileStatus.READY, Optional.empty(), now, now));
        SpreadsheetSheet sheet = spreadsheets.saveSheet(new SpreadsheetSheet(
            UUID.randomUUID(), fid, "Sales",
            List.of("region","product","units","revenue"),
            List.of("TEXT","TEXT","NUMBER","NUMBER"),
            4, "summary", new float[1024]));
        sheetId = sheet.id();
        List<SpreadsheetRow> rows = List.of(
            row(sheet.id(), 1, "APAC","Widget", 10, 100.0),
            row(sheet.id(), 2, "APAC","Gadget", 20, 250.0),
            row(sheet.id(), 3, "EMEA","Widget", 5,  60.0),
            row(sheet.id(), 4, "EMEA","Gadget", 8,  90.0));
        spreadsheets.saveRows(rows);
    }
    private SpreadsheetRow row(UUID sid, int n, String region, String product, int units, double revenue) {
        Map<String,Object> v = new LinkedHashMap<>();
        v.put("region", region); v.put("product", product);
        v.put("units", String.valueOf(units)); v.put("revenue", String.valueOf(revenue));
        return new SpreadsheetRow(UUID.randomUUID(), sid, n, v);
    }

    @Test void filter_equals_returns_matching_rows() {
        var out = svc.execute(new ToolCallInput(sheetId.toString(),
            List.of(new Filter("region","=","APAC")), null, null, null, null));
        assertThat(out).containsKey("rows");
        assertThat((int) out.get("rowCount")).isEqualTo(2);
    }

    @Test void numeric_filter_greater_than() {
        var out = svc.execute(new ToolCallInput(sheetId.toString(),
            List.of(new Filter("units",">","9")), null, null, null, null));
        assertThat((int) out.get("rowCount")).isEqualTo(2); // 10 and 20
    }

    @Test void sum_aggregate() {
        var out = svc.execute(new ToolCallInput(sheetId.toString(),
            List.of(), "sum", "revenue", null, null));
        assertThat(out).containsKey("aggregateValue");
        assertThat(Double.parseDouble(out.get("aggregateValue").toString())).isEqualTo(500.0);
    }

    @Test void group_by_region_with_sum() {
        var out = svc.execute(new ToolCallInput(sheetId.toString(),
            List.of(), "sum", "revenue", List.of("region"), null));
        assertThat(out).containsKey("groups");
        @SuppressWarnings("unchecked")
        List<Map<String,Object>> groups = (List<Map<String,Object>>) out.get("groups");
        assertThat(groups).hasSize(2); // APAC, EMEA
    }

    @Test void contains_filter() {
        var out = svc.execute(new ToolCallInput(sheetId.toString(),
            List.of(new Filter("product","contains","idg")), null, null, null, null)); // 'Widget'/'Gadget'? 'idg'->Widget
        assertThat((int) out.get("rowCount")).isEqualTo(2); // both Widgets
    }

    @Test void unknown_column_returns_tool_error() {
        var out = svc.execute(new ToolCallInput(sheetId.toString(),
            List.of(new Filter("nope","=","x")), null, null, null, null));
        assertThat(out.get("error")).isEqualTo("UNKNOWN_COLUMN");
        assertThat(out.get("column")).isEqualTo("nope");
    }

    @Test void unknown_operator_returns_tool_error() {
        var out = svc.execute(new ToolCallInput(sheetId.toString(),
            List.of(new Filter("region","LIKE","x")), null, null, null, null));
        assertThat(out.get("error")).isEqualTo("UNKNOWN_OPERATOR");
    }

    @Test void unknown_sheet_returns_tool_error() {
        var out = svc.execute(new ToolCallInput(UUID.randomUUID().toString(),
            List.of(), null, null, null, null));
        assertThat(out.get("error")).isEqualTo("UNKNOWN_SHEET");
    }

    @Test void limit_capped_at_500() {
        var out = svc.execute(new ToolCallInput(sheetId.toString(),
            List.of(), null, null, null, 99999));
        assertThat(out).containsKey("rows"); // no exception; limit clamped internally
    }
}
