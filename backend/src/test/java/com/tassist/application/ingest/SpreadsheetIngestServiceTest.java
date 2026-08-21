package com.tassist.application.ingest;

import com.tassist.domain.model.SpreadsheetRow;
import com.tassist.domain.model.SpreadsheetSheet;
import com.tassist.domain.port.out.EmbeddingClient;
import com.tassist.domain.port.out.SpreadsheetRepository;
import com.tassist.domain.vo.FileId;
import com.tassist.domain.vo.FileType;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Unit test for §11.3 ingestion orchestration using fakes (no Spring, no DB). */
class SpreadsheetIngestServiceTest {

    // fake embedder: fixed 1024-dim vector, records calls
    static class FakeEmbedder implements EmbeddingClient {
        int embedCalls = 0;
        public float[] embed(String text) { embedCalls++; return new float[1024]; }
        public List<float[]> embedBatch(List<String> texts) { return texts.stream().map(t -> new float[1024]).toList(); }
        public int dimension() { return 1024; }
    }

    // fake repo: captures saved sheets and rows
    static class FakeRepo implements SpreadsheetRepository {
        final List<SpreadsheetSheet> sheets = new ArrayList<>();
        final List<SpreadsheetRow> rows = new ArrayList<>();
        int saveRowsCalls = 0;
        public SpreadsheetSheet saveSheet(SpreadsheetSheet s) { sheets.add(s); return s; }
        public void saveRows(List<SpreadsheetRow> r) { saveRowsCalls++; rows.addAll(r); }
        public List<SpreadsheetSheet> findSheetsByFile(FileId f) { return sheets; }
        public java.util.Optional<SpreadsheetSheet> findSheetById(java.util.UUID id) { return java.util.Optional.empty(); }
        public long countRowsBySheet(UUID id) { return rows.size(); }
        public void deleteByFile(FileId f) {}
        public List<ScoredSheet> searchSimilarSheets(float[] q, List<FileId> ids, int k) { return List.of(); }
    }

    private SpreadsheetIngestService service(FakeEmbedder e, FakeRepo repo) {
        return new SpreadsheetIngestService(new SpreadsheetParser(), new SchemaSummarizer(), e, repo);
    }

    @Test void csv_ingest_saves_sheet_and_rows_with_embedding() {
        FakeEmbedder e = new FakeEmbedder(); FakeRepo repo = new FakeRepo();
        String csv = "product,units\nA,10\nB,20\nC,30\n";
        long total = service(e, repo).ingest(FileId.newId(), FileType.CSV, csv.getBytes(StandardCharsets.UTF_8));

        assertThat(total).isEqualTo(3);
        assertThat(repo.sheets).hasSize(1);
        assertThat(repo.sheets.get(0).schemaSummaryEmbedding()).hasSize(1024);
        assertThat(e.embedCalls).isEqualTo(1);           // one embed per sheet summary
        assertThat(repo.rows).hasSize(3);
        assertThat(repo.rows.get(0).rowNumber()).isEqualTo(1L); // 1-indexed
        assertThat(repo.rows.get(0).values()).containsEntry("product", "A");
    }

    @Test void rows_are_persisted_in_batches() {
        FakeEmbedder e = new FakeEmbedder(); FakeRepo repo = new FakeRepo();
        StringBuilder csv = new StringBuilder("id,val\n");
        for (int i = 0; i < 2500; i++) csv.append(i).append(",x\n");
        long total = service(e, repo).ingest(FileId.newId(), FileType.CSV, csv.toString().getBytes(StandardCharsets.UTF_8));

        assertThat(total).isEqualTo(2500);
        assertThat(repo.rows).hasSize(2500);
        assertThat(repo.saveRowsCalls).isEqualTo(3);     // 1000 + 1000 + 500
    }
}
