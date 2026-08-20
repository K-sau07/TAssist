package com.tassist.infrastructure.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "spreadsheet_row")
public class SpreadsheetRowEntity {
    @Id private UUID id;
    @Column(name = "sheet_id", nullable = false) private UUID sheetId;
    @Column(name = "row_number", nullable = false) private long rowNumber;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "values", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> values;

    public SpreadsheetRowEntity() {}
    public UUID getId() { return id; } public void setId(UUID v) { this.id = v; }
    public UUID getSheetId() { return sheetId; } public void setSheetId(UUID v) { this.sheetId = v; }
    public long getRowNumber() { return rowNumber; } public void setRowNumber(long v) { this.rowNumber = v; }
    public Map<String, Object> getValues() { return values; } public void setValues(Map<String, Object> v) { this.values = v; }
}
