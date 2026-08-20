package com.tassist.infrastructure.persistence.mapper;

import com.tassist.domain.model.SpreadsheetSheet;
import com.tassist.domain.vo.FileId;
import com.tassist.infrastructure.persistence.entity.SpreadsheetSheetEntity;

public final class SpreadsheetSheetMapper {
    private SpreadsheetSheetMapper() {}

    public static SpreadsheetSheetEntity toEntity(SpreadsheetSheet s) {
        SpreadsheetSheetEntity e = new SpreadsheetSheetEntity();
        e.setId(s.id());
        e.setFileId(s.fileId().value());
        e.setSheetName(s.sheetName());
        e.setColumnNames(s.columnNames());
        e.setColumnTypes(s.columnTypes());
        e.setRowCount(s.rowCount());
        e.setSchemaSummary(s.schemaSummary());
        e.setSchemaSummaryEmbedding(s.schemaSummaryEmbedding());
        return e;
    }

    public static SpreadsheetSheet toDomain(SpreadsheetSheetEntity e) {
        return new SpreadsheetSheet(
            e.getId(),
            FileId.of(e.getFileId()),
            e.getSheetName(),
            e.getColumnNames(),
            e.getColumnTypes(),
            e.getRowCount(),
            e.getSchemaSummary(),
            e.getSchemaSummaryEmbedding()
        );
    }
}
