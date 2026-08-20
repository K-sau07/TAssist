package com.tassist.infrastructure.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "spreadsheet_sheet")
public class SpreadsheetSheetEntity {
    @Id private UUID id;
    @Column(name = "file_id", nullable = false) private UUID fileId;
    @Column(name = "sheet_name", nullable = false) private String sheetName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "column_names", nullable = false, columnDefinition = "jsonb")
    private List<String> columnNames;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "column_types", nullable = false, columnDefinition = "jsonb")
    private List<String> columnTypes;

    @Column(name = "row_count", nullable = false) private long rowCount;
    @Column(name = "schema_summary", nullable = false) private String schemaSummary;

    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = 1024)
    @Column(name = "schema_summary_embedding", nullable = false, columnDefinition = "vector(1024)")
    private float[] schemaSummaryEmbedding;

    public SpreadsheetSheetEntity() {}
    public UUID getId() { return id; } public void setId(UUID v) { this.id = v; }
    public UUID getFileId() { return fileId; } public void setFileId(UUID v) { this.fileId = v; }
    public String getSheetName() { return sheetName; } public void setSheetName(String v) { this.sheetName = v; }
    public List<String> getColumnNames() { return columnNames; } public void setColumnNames(List<String> v) { this.columnNames = v; }
    public List<String> getColumnTypes() { return columnTypes; } public void setColumnTypes(List<String> v) { this.columnTypes = v; }
    public long getRowCount() { return rowCount; } public void setRowCount(long v) { this.rowCount = v; }
    public String getSchemaSummary() { return schemaSummary; } public void setSchemaSummary(String v) { this.schemaSummary = v; }
    public float[] getSchemaSummaryEmbedding() { return schemaSummaryEmbedding; } public void setSchemaSummaryEmbedding(float[] v) { this.schemaSummaryEmbedding = v; }
}
