package com.tassist.application.retrieval;

import com.tassist.domain.error.Forbidden;
import com.tassist.domain.error.ValidationError;
import com.tassist.domain.model.*;
import com.tassist.domain.port.in.RetrievalUseCase.*;
import com.tassist.domain.port.out.*;
import com.tassist.domain.port.out.ChunkRepository.ScoredChunk;
import com.tassist.domain.port.out.SpreadsheetRepository.ScoredSheet;
import com.tassist.domain.vo.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

/** Unit tests for §11.4 scope selection + thresholding (fakes, no Spring/DB). */
class RetrievalServiceTest {

    // --- fakes ---
    static class FakeEmbed implements EmbeddingClient {
        public float[] embed(String t) { return new float[]{1f}; }
        public List<float[]> embedBatch(List<String> t) { return List.of(); }
        public int dimension() { return 1; }
    }
    static class FakeChunks implements ChunkRepository {
        List<ScoredChunk> toReturn = List.of();
        List<FileId> lastCandidates; int lastTopK;
        public void saveAll(List<Chunk> c) {}
        public long countByFile(FileId f) { return 0; }
        public void deleteByFile(FileId f) {}
        public List<ScoredChunk> searchSimilar(float[] q, List<FileId> cand, int topK) {
            lastCandidates = cand; lastTopK = topK; return toReturn; }
    }
    static class FakeSheets implements SpreadsheetRepository {
        List<ScoredSheet> toReturn = List.of();
        public SpreadsheetSheet saveSheet(SpreadsheetSheet s) { return s; }
        public void saveRows(List<SpreadsheetRow> r) {}
        public List<SpreadsheetSheet> findSheetsByFile(FileId f) { return List.of(); }
        public long countRowsBySheet(UUID id) { return 0; }
        public void deleteByFile(FileId f) {}
        public List<ScoredSheet> searchSimilarSheets(float[] q, List<FileId> cand, int k) { return toReturn; }
    }
    static class FakeFolders implements FolderRepository {
        final Map<UUID, Folder> byId = new HashMap<>();
        public Folder save(Folder f) { byId.put(f.id().value(), f); return f; }
        public Optional<Folder> findById(FolderId id) { return Optional.ofNullable(byId.get(id.value())); }
        public List<Folder> findByOwner(UserId o) { return List.of(); }
        public boolean existsByOwnerAndName(UserId o, String n) { return false; }
        public void delete(FolderId id) {}
    }
    static class FakeFolderFiles implements FolderFileRepository {
        final Map<UUID, List<FileId>> byFolder = new HashMap<>();
        public FolderFile add(FolderFile ff) { return ff; }
        public void remove(FolderId fo, FileId fi) {}
        public List<FileId> findFileIdsByFolder(FolderId fo) { return byFolder.getOrDefault(fo.value(), List.of()); }
        public List<FolderId> findFolderIdsByFile(FileId fi) { return List.of(); }
    }
    static class FakeFiles implements FileRepository {
        final Map<UUID, File> byId = new HashMap<>();
        public File save(File f) { byId.put(f.id().value(), f); return f; }
        public Optional<File> findById(FileId id) { return Optional.ofNullable(byId.get(id.value())); }
        public List<File> findByOwner(UserId o) { return List.of(); }
        public Optional<File> findByOwnerAndContentHash(UserId o, String h) { return Optional.empty(); }
        public List<File> findByOwnerAndFilename(UserId o, String n) { return List.of(); }
        public void delete(FileId id) {}
    }

    private final FakeEmbed embed = new FakeEmbed();
    private final FakeChunks chunks = new FakeChunks();
    private final FakeSheets sheets = new FakeSheets();
    private final FakeFolders folders = new FakeFolders();
    private final FakeFolderFiles folderFiles = new FakeFolderFiles();
    private final FakeFiles files = new FakeFiles();
    private final RetrievalService svc =
        new RetrievalService(embed, chunks, sheets, folders, folderFiles, files);

    private final UserId user = UserId.newId();

    private FileId seedFile(UserId owner) {
        FileId id = FileId.newId();
        files.save(new File(id, owner, "f.pdf", FileType.PDF, 1, "k", "h" + id.value(),
            FileStatus.READY, Optional.empty(), Instant.now(), Instant.now()));
        return id;
    }
    private ScoredChunk chunk(FileId f, double sim) {
        return new ScoredChunk(new Chunk(ChunkId.newId(), f, 0, "text", Map.of(), new float[]{1f}), sim);
    }
    private RetrievalQuery q(Scope s, Optional<FolderId> fo, List<FileId> mentions) {
        return new RetrievalQuery(user, "question?", s, fo, Optional.empty(), mentions);
    }

    @Test void regular_scope_returns_empty_no_search() {
        var r = svc.retrieve(q(Scope.REGULAR, Optional.empty(), List.of()));
        assertThat(r.textHits()).isEmpty();
        assertThat(r.spreadsheetHits()).isEmpty();
        assertThat(chunks.lastCandidates).isNull(); // never searched
    }

    @Test void channel_scope_not_yet_supported() {
        assertThatThrownBy(() -> svc.retrieve(q(Scope.CHANNEL, Optional.empty(), List.of())))
            .isInstanceOf(ValidationError.class);
    }

    @Test void folder_scope_uses_folder_files_and_topk_6() {
        FileId f = seedFile(user);
        Folder folder = new Folder(FolderId.newId(), user, "F", Instant.now());
        folders.save(folder);
        folderFiles.byFolder.put(folder.id().value(), List.of(f));
        chunks.toReturn = List.of(chunk(f, 0.9));
        var r = svc.retrieve(q(Scope.FOLDER, Optional.of(folder.id()), List.of()));
        assertThat(chunks.lastCandidates).containsExactly(f);
        assertThat(chunks.lastTopK).isEqualTo(6);
        assertThat(r.textHits()).hasSize(1);
    }

    @Test void folder_not_owned_is_forbidden() {
        Folder folder = new Folder(FolderId.newId(), UserId.newId(), "F", Instant.now());
        folders.save(folder);
        assertThatThrownBy(() -> svc.retrieve(q(Scope.FOLDER, Optional.of(folder.id()), List.of())))
            .isInstanceOf(Forbidden.class);
    }

    @Test void mentions_override_scope_and_use_topk_8() {
        FileId mentioned = seedFile(user);
        Folder folder = new Folder(FolderId.newId(), user, "F", Instant.now());
        folders.save(folder);
        folderFiles.byFolder.put(folder.id().value(), List.of(seedFile(user)));
        chunks.toReturn = List.of(chunk(mentioned, 0.8));
        // scope says FOLDER but mentions present -> mentions win
        var r = svc.retrieve(q(Scope.FOLDER, Optional.of(folder.id()), List.of(mentioned)));
        assertThat(chunks.lastCandidates).containsExactly(mentioned);
        assertThat(chunks.lastTopK).isEqualTo(8);
    }

    @Test void mention_of_unowned_file_is_warned_and_dropped() {
        FileId other = seedFile(UserId.newId()); // owned by someone else
        var r = svc.retrieve(q(Scope.MENTIONS, Optional.empty(), List.of(other)));
        assertThat(r.warnings()).hasSize(1);
        assertThat(chunks.lastCandidates).isNull(); // no candidates -> no search
    }

    @Test void below_threshold_hits_dropped_and_flagged() {
        FileId f = seedFile(user);
        chunks.toReturn = List.of(chunk(f, 0.3)); // below 0.4 floor
        var r = svc.retrieve(q(Scope.MENTIONS, Optional.empty(), List.of(f)));
        assertThat(r.textHits()).isEmpty();
        assertThat(r.allBelowThreshold()).isTrue();
    }

    @Test void at_threshold_kept() {
        FileId f = seedFile(user);
        chunks.toReturn = List.of(chunk(f, 0.4)); // exactly floor -> kept (>=)
        var r = svc.retrieve(q(Scope.MENTIONS, Optional.empty(), List.of(f)));
        assertThat(r.textHits()).hasSize(1);
        assertThat(r.allBelowThreshold()).isFalse();
    }
}
