package com.tassist.application.spreadsheet;

import com.tassist.domain.model.SpreadsheetSheet;
import com.tassist.domain.port.out.SpreadsheetRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Executes the `query_spreadsheet` tool (§11.7) as safe parameterized SQL over spreadsheet_row.values (JSONB).
 * Column names are validated against the sheet's declared columns (never taken raw from the model);
 * operators are whitelist-only; values are bound as JDBC parameters. Never lets model text reach SQL directly.
 */
@Service
public class SpreadsheetQueryService {

    private static final Logger log = LoggerFactory.getLogger(SpreadsheetQueryService.class);
    private static final int MAX_LIMIT = 500;
    private static final int DEFAULT_LIMIT = 50;

    private static final Map<String, String> OPS = Map.of(
        "=", "=", "!=", "<>", "<", "<", "<=", "<=", ">", ">", ">=", ">=",
        "contains", "ILIKE", "in", "IN");
    private static final Set<String> AGGS = Set.of("count", "sum", "avg", "min", "max");

    @PersistenceContext private EntityManager em;
    private final SpreadsheetRepository spreadsheets;

    public SpreadsheetQueryService(SpreadsheetRepository spreadsheets) { this.spreadsheets = spreadsheets; }

    // --- inputs (mirror the §11.5 tool input_schema) ---
    public record Filter(String column, String op, Object value) {}
    public record ToolCallInput(String sheetId, List<Filter> filters, String aggregate,
                                String aggregateColumn, List<String> groupBy, Integer limit) {}

    /** Result: either {rows,rowCount}, {aggregateValue}, or {groups}, or {error,...}. */
    @Transactional(readOnly = true)
    public Map<String, Object> execute(ToolCallInput in) {
        UUID sheetId;
        try { sheetId = UUID.fromString(in.sheetId()); }
        catch (Exception e) { return err("UNKNOWN_SHEET", "sheet_id", in.sheetId()); }

        SpreadsheetSheet sheet = spreadsheets.findSheetById(sheetId).orElse(null);
        if (sheet == null) return err("UNKNOWN_SHEET", "sheet_id", in.sheetId());
        Set<String> cols = new HashSet<>(sheet.columnNames());

        // validate columns referenced anywhere
        List<Filter> filters = in.filters() == null ? List.of() : in.filters();
        for (Filter f : filters) {
            if (f.column() == null || !cols.contains(f.column())) return err("UNKNOWN_COLUMN", "column", f.column());
            if (f.op() == null || !OPS.containsKey(f.op())) return err("UNKNOWN_OPERATOR", "op", f.op());
        }
        List<String> groupBy = in.groupBy() == null ? List.of() : in.groupBy();
        for (String g : groupBy) if (!cols.contains(g)) return err("UNKNOWN_COLUMN", "column", g);
        String aggCol = in.aggregateColumn();
        if (aggCol != null && !cols.contains(aggCol)) return err("UNKNOWN_COLUMN", "column", aggCol);
        if (in.aggregate() != null && !AGGS.contains(in.aggregate()))
            return err("UNKNOWN_AGGREGATE", "aggregate", in.aggregate());

        // build WHERE (parameterized)
        StringBuilder where = new StringBuilder("sheet_id = :sid");
        Map<String, Object> params = new HashMap<>();
        params.put("sid", sheetId);
        int p = 0;
        for (Filter f : filters) {
            String pname = "p" + (p++);
            String jsonCol = "(values ->> '" + f.column() + "')"; // column already whitelisted
            String sqlOp = OPS.get(f.op());
            if ("in".equals(f.op())) {
                where.append(" AND ").append(jsonCol).append(" IN (:").append(pname).append(")");
                params.put(pname, f.value() instanceof Collection<?> c ? c : List.of(String.valueOf(f.value())));
            } else if ("contains".equals(f.op())) {
                where.append(" AND ").append(jsonCol).append(" ILIKE :").append(pname);
                params.put(pname, "%" + f.value() + "%");
            } else {
                // numeric-aware compare for </<=/>/>=, else text
                boolean numeric = List.of("<", "<=", ">", ">=").contains(f.op());
                if (numeric) {
                    where.append(" AND (").append(jsonCol).append(")::numeric ").append(sqlOp)
                         .append(" :").append(pname);
                    params.put(pname, toNumber(f.value()));
                } else {
                    where.append(" AND ").append(jsonCol).append(" ").append(sqlOp).append(" :").append(pname);
                    params.put(pname, String.valueOf(f.value()));
                }
            }
        }

        try {
            if (in.aggregate() != null && groupBy.isEmpty()) return aggregate(in, where.toString(), params);
            if (!groupBy.isEmpty()) return grouped(in, groupBy, where.toString(), params);
            return rows(in, where.toString(), params);
        } catch (RuntimeException e) {
            log.warn("query_spreadsheet failed: {}", e.toString());
            return err("QUERY_ERROR", "message", rootMsg(e));
        }
    }

    private Map<String, Object> rows(ToolCallInput in, String where, Map<String, Object> params) {
        int limit = clampLimit(in.limit());
        String sql = "SELECT values FROM spreadsheet_row WHERE " + where + " ORDER BY row_number LIMIT " + limit;
        Query q = em.createNativeQuery(sql);
        params.forEach(q::setParameter);
        @SuppressWarnings("unchecked") List<Object> raw = q.getResultList();
        List<Object> rows = new ArrayList<>(raw.size());
        for (Object o : raw) rows.add(o); // JSONB comes back as String/PGobject; passed through to tool_result
        log.info("query_spreadsheet rows: {} -> {} rows", where, rows.size());
        return Map.of("rows", rows, "rowCount", rows.size());
    }

    private Map<String, Object> aggregate(ToolCallInput in, String where, Map<String, Object> params) {
        String expr = aggExpr(in.aggregate(), in.aggregateColumn());
        String sql = "SELECT " + expr + " AS v FROM spreadsheet_row WHERE " + where;
        Query q = em.createNativeQuery(sql);
        params.forEach(q::setParameter);
        Object v = q.getSingleResult();
        log.info("query_spreadsheet aggregate {}: {}", in.aggregate(), v);
        Map<String, Object> out = new HashMap<>();
        out.put("aggregateValue", v);
        return out;
    }

    private Map<String, Object> grouped(ToolCallInput in, List<String> groupBy, String where,
                                        Map<String, Object> params) {
        String keyExprs = String.join(", ",
            groupBy.stream().map(g -> "(values ->> '" + g + "')").toList());
        String valExpr = in.aggregate() != null ? aggExpr(in.aggregate(), in.aggregateColumn()) : "count(*)";
        String sql = "SELECT " + keyExprs + ", " + valExpr + " AS v FROM spreadsheet_row WHERE " + where
            + " GROUP BY " + keyExprs + " LIMIT " + clampLimit(in.limit());
        Query q = em.createNativeQuery(sql, Tuple.class);
        params.forEach(q::setParameter);
        @SuppressWarnings("unchecked") List<Tuple> res = q.getResultList();
        List<Map<String, Object>> groups = new ArrayList<>(res.size());
        for (Tuple t : res) {
            Map<String, Object> key = new LinkedHashMap<>();
            for (int i = 0; i < groupBy.size(); i++) key.put(groupBy.get(i), t.get(i));
            groups.add(Map.of("key", key, "value", t.get(groupBy.size())));
        }
        log.info("query_spreadsheet grouped by {}: {} groups", groupBy, groups.size());
        return Map.of("groups", groups);
    }

    private String aggExpr(String agg, String col) {
        if ("count".equals(agg)) return "count(*)";
        if (col == null) throw new IllegalArgumentException("aggregate_column required for " + agg);
        String numeric = "(values ->> '" + col + "')::numeric"; // col whitelisted
        return switch (agg) {
            case "sum" -> "sum(" + numeric + ")";
            case "avg" -> "avg(" + numeric + ")";
            case "min" -> "min(" + numeric + ")";
            case "max" -> "max(" + numeric + ")";
            default -> throw new IllegalArgumentException("bad aggregate " + agg);
        };
    }

    private int clampLimit(Integer limit) {
        if (limit == null) return DEFAULT_LIMIT;
        return Math.max(1, Math.min(limit, MAX_LIMIT));
    }
    private Object toNumber(Object v) {
        try { return Double.parseDouble(String.valueOf(v)); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("non-numeric comparison value: " + v); }
    }
    private Map<String, Object> err(String code, String field, String val) {
        Map<String, Object> m = new HashMap<>();
        m.put("error", code); m.put(field, val);
        return m;
    }
    private String rootMsg(Throwable e) {
        Throwable r = e; while (r.getCause() != null) r = r.getCause();
        return r.getMessage();
    }
}
