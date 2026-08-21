package com.tassist.application.ingest;

import com.tassist.domain.model.SpreadsheetRow;
import com.tassist.domain.model.SpreadsheetSheet;
import com.tassist.domain.port.out.EmbeddingClient;
import com.tassist.domain.port.out.SpreadsheetRepository;
import com.tassist.domain.vo.FileId;
import com.tassist.domain.vo.FileType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Structured spreadsheet ingestion (§11.3): parse → per sheet build metadata + schema summary →
 * embed summary → saveSheet → saveRows (batched). Replaces the Step-4 plain-dump / D16 hold.
 * Throws on failure; the caller (FileService) marks the file FAILED.
 */
@Service
public class SpreadsheetIngestService {

    private static final Logger log = LoggerFactory.getLogger(SpreadsheetIngestService.class);
    private static final int ROW_BATCH = 1000;

    private final SpreadsheetParser parser;
    private final SchemaSummarizer summarizer;
    private final EmbeddingClient embeddings;
    private final SpreadsheetRepository spreadsheets;

    public SpreadsheetIngestService(SpreadsheetParser parser, SchemaSummarizer summarizer,
                                    EmbeddingClient embeddings, SpreadsheetRepository spreadsheets) {
        this.parser = parser;
        this.summarizer = summarizer;
        this.embeddings = embeddings;
        this.spreadsheets = spreadsheets;
    }

    /** Ingest all sheets of one spreadsheet file. Returns total rows persisted across sheets. */
    public long ingest(FileId fileId, FileType type, byte[] content) {
        List<ParsedSheet> sheets = parser.parse(type, content);
        long totalRows = 0;
        for (ParsedSheet parsed : sheets) {
            String summary = summarizer.summarize(parsed);
            float[] embedding = embeddings.embed(summary); // single-summary embed (§11.3 step 6)

            SpreadsheetSheet sheet = new SpreadsheetSheet(
                UUID.randomUUID(), fileId, parsed.sheetName(),
                parsed.columnNames(),
                parsed.columnTypes().stream().map(Enum::name).toList(),
                parsed.rowCount(), summary, embedding);
            SpreadsheetSheet saved = spreadsheets.saveSheet(sheet);

            persistRows(saved.id(), parsed);
            totalRows += parsed.rowCount();
            log.info("Ingested sheet '{}' of file {}: {} rows, {} cols, summary embedded ({}-dim).",
                parsed.sheetName(), fileId.value(), parsed.rowCount(),
                parsed.columnNames().size(), embeddings.dimension());
        }
        return totalRows;
    }

    private void persistRows(UUID sheetId, ParsedSheet parsed) {
        List<Map<String, Object>> src = parsed.rows();
        List<SpreadsheetRow> batch = new ArrayList<>(ROW_BATCH);
        for (int i = 0; i < src.size(); i++) {
            batch.add(new SpreadsheetRow(UUID.randomUUID(), sheetId, i + 1L, src.get(i)));
            if (batch.size() == ROW_BATCH) {
                spreadsheets.saveRows(batch);
                batch = new ArrayList<>(ROW_BATCH);
            }
        }
        if (!batch.isEmpty()) spreadsheets.saveRows(batch);
    }
}
