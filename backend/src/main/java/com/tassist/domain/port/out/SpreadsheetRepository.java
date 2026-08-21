package com.tassist.domain.port.out;

import com.tassist.domain.model.SpreadsheetRow;
import com.tassist.domain.model.SpreadsheetSheet;
import com.tassist.domain.vo.FileId;
import java.util.List;
import java.util.UUID;

/** Persistence + schema-vector-search port for spreadsheet structured mode (spec §7, §11.3, §11.4 step 6). */
public interface SpreadsheetRepository {
    SpreadsheetSheet saveSheet(SpreadsheetSheet sheet);
    void saveRows(List<SpreadsheetRow> rows);
    List<SpreadsheetSheet> findSheetsByFile(FileId fileId);
    java.util.Optional<SpreadsheetSheet> findSheetById(UUID sheetId);
    long countRowsBySheet(UUID sheetId);
    void deleteByFile(FileId fileId);

    /** Schema-summary similarity search restricted to candidate files (§11.4 step 6). */
    List<ScoredSheet> searchSimilarSheets(float[] queryEmbedding, List<FileId> candidateFileIds, int topK);

    record ScoredSheet(SpreadsheetSheet sheet, double similarity) {}
}
