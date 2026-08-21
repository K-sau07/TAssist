package com.tassist.application.retrieval;

import com.tassist.domain.error.Forbidden;
import com.tassist.domain.error.NotFoundError;
import com.tassist.domain.error.ValidationError;
import com.tassist.domain.model.File;
import com.tassist.domain.model.Folder;
import com.tassist.domain.port.in.RetrievalUseCase;
import com.tassist.domain.port.out.ChunkRepository;
import com.tassist.domain.port.out.ChunkRepository.ScoredChunk;
import com.tassist.domain.port.out.EmbeddingClient;
import com.tassist.domain.port.out.FileRepository;
import com.tassist.domain.port.out.FolderFileRepository;
import com.tassist.domain.port.out.FolderRepository;
import com.tassist.domain.port.out.SpreadsheetRepository;
import com.tassist.domain.port.out.SpreadsheetRepository.ScoredSheet;
import com.tassist.domain.vo.FileId;
import com.tassist.domain.vo.FolderId;
import com.tassist.domain.vo.UserId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Grounded retrieval (§11.4). REGULAR → empty. FOLDER/MENTIONS resolve candidate files
 * (ownership enforced), embed the query once, run text + spreadsheet vector search,
 * threshold at 0.4. CHANNEL scope is deferred until channels are built (Step 12+).
 * @mention name→id resolution happens upstream; this receives resolved mentionedFileIds.
 */
@Service
public class RetrievalService implements RetrievalUseCase {

    private static final Logger log = LoggerFactory.getLogger(RetrievalService.class);
    private static final double SIMILARITY_FLOOR = 0.4;
    private static final int TOPK_SCOPED = 6;    // folder/channel
    private static final int TOPK_MENTIONS = 8;  // mentions pull more (2-3 files)
    private static final int TOPK_SHEETS = 3;

    private final EmbeddingClient embeddings;
    private final ChunkRepository chunks;
    private final SpreadsheetRepository spreadsheets;
    private final FolderRepository folders;
    private final FolderFileRepository folderFiles;
    private final FileRepository files;

    public RetrievalService(EmbeddingClient embeddings, ChunkRepository chunks,
                            SpreadsheetRepository spreadsheets, FolderRepository folders,
                            FolderFileRepository folderFiles, FileRepository files) {
        this.embeddings = embeddings;
        this.chunks = chunks;
        this.spreadsheets = spreadsheets;
        this.folders = folders;
        this.folderFiles = folderFiles;
        this.files = files;
    }

    @Override
    public RetrievalResult retrieve(RetrievalQuery q) {
        // 1. REGULAR: no retrieval.
        if (q.scope() == Scope.REGULAR) {
            return new RetrievalResult(List.of(), List.of(), false, List.of());
        }
        if (q.scope() == Scope.CHANNEL) {
            throw new ValidationError("CHANNEL-scope retrieval is not yet supported");
        }

        List<String> warnings = new ArrayList<>();

        // 2+4. Resolve candidate files. Mentions override scope (§11.4 step 2).
        int topK;
        List<FileId> candidates;
        if (!q.mentionedFileIds().isEmpty()) {
            candidates = visibleOwnedFiles(q.userId(), q.mentionedFileIds(), warnings);
            topK = TOPK_MENTIONS;
        } else if (q.scope() == Scope.FOLDER) {
            candidates = folderCandidates(q.userId(), q.folderId()
                .orElseThrow(() -> new ValidationError("folderId required for FOLDER scope")));
            topK = TOPK_SCOPED;
        } else { // MENTIONS scope but no ids
            candidates = List.of();
            topK = TOPK_MENTIONS;
        }

        if (candidates.isEmpty()) {
            return new RetrievalResult(List.of(), List.of(), false, warnings);
        }

        // 3. Query embedding (single call).
        float[] queryEmb = embeddings.embed(q.question());

        // 5+6. Vector search: text chunks + spreadsheet schemas.
        List<ScoredChunk> chunkHits = chunks.searchSimilar(queryEmb, candidates, topK);
        List<ScoredSheet> sheetHits = spreadsheets.searchSimilarSheets(queryEmb, candidates, TOPK_SHEETS);

        // 7. Threshold at 0.4.
        List<TextHit> textHits = chunkHits.stream()
            .filter(h -> h.similarity() >= SIMILARITY_FLOOR)
            .map(h -> new TextHit(h.chunk(), h.similarity()))
            .toList();
        List<SpreadsheetHit> spreadsheetHits = sheetHits.stream()
            .filter(h -> h.similarity() >= SIMILARITY_FLOOR)
            .map(h -> new SpreadsheetHit(h.sheet(), h.similarity()))
            .toList();

        boolean anyRaw = !chunkHits.isEmpty() || !sheetHits.isEmpty();
        boolean allBelow = anyRaw && textHits.isEmpty() && spreadsheetHits.isEmpty();

        log.info("retrieve: user={} scope={} candidates={} textHits={} sheetHits={} allBelowThreshold={}",
            q.userId().value(), q.scope(), candidates.size(), textHits.size(), spreadsheetHits.size(), allBelow);

        return new RetrievalResult(textHits, spreadsheetHits, allBelow, warnings);
    }

    /** FOLDER scope: files in the folder, folder owned by user (§11.4 step 4). */
    private List<FileId> folderCandidates(UserId userId, FolderId folderId) {
        Folder folder = folders.findById(folderId)
            .orElseThrow(() -> new NotFoundError("folder not found"));
        if (!folder.ownerId().equals(userId)) {
            throw new Forbidden("not your folder");
        }
        return folderFiles.findFileIdsByFolder(folderId);
    }

    /** MENTIONS: keep only files owned by the user; drop others silently-but-warned. */
    private List<FileId> visibleOwnedFiles(UserId userId, List<FileId> ids, List<String> warnings) {
        List<FileId> ok = new ArrayList<>(ids.size());
        for (FileId id : ids) {
            File f = files.findById(id).orElse(null);
            if (f != null && f.ownerId().equals(userId)) {
                ok.add(id);
            } else {
                warnings.add("File " + id.value() + " not accessible; ignoring.");
            }
        }
        return ok;
    }
}
