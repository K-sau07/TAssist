package com.tassist.infrastructure.persistence.mapper;

import com.tassist.domain.model.SpreadsheetRow;
import com.tassist.infrastructure.persistence.entity.SpreadsheetRowEntity;

public final class SpreadsheetRowMapper {
    private SpreadsheetRowMapper() {}

    public static SpreadsheetRowEntity toEntity(SpreadsheetRow r) {
        SpreadsheetRowEntity e = new SpreadsheetRowEntity();
        e.setId(r.id());
        e.setSheetId(r.sheetId());
        e.setRowNumber(r.rowNumber());
        e.setValues(r.values());
        return e;
    }

    public static SpreadsheetRow toDomain(SpreadsheetRowEntity e) {
        return new SpreadsheetRow(e.getId(), e.getSheetId(), e.getRowNumber(), e.getValues());
    }
}
