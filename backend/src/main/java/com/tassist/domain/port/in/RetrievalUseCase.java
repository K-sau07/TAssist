package com.tassist.domain.port.in;

import com.tassist.domain.model.Chunk;
import com.tassist.domain.model.SpreadsheetSheet;
import com.tassist.domain.vo.ChannelId;
import com.tassist.domain.vo.FileId;
import com.tassist.domain.vo.FolderId;
import com.tassist.domain.vo.UserId;
import java.util.List;
import java.util.Optional;

/**
 * Inbound port: grounded retrieval (spec 11.4). Built in Step 8. Returns text-chunk hits
 * and spreadsheet-schema hits, plus whether everything fell below the 0.4 similarity floor.
 */
public interface RetrievalUseCase {

    RetrievalResult retrieve(RetrievalQuery query);

    enum Scope { REGULAR, FOLDER, CHANNEL, MENTIONS }

    record RetrievalQuery(
            UserId userId,
            String question,
            Scope scope,
            Optional<FolderId> folderId,
            Optional<ChannelId> channelId,
            List<FileId> mentionedFileIds
    ) {
        public RetrievalQuery {
            if (userId == null) throw new IllegalArgumentException("RetrievalQuery.userId must not be null");
            if (question == null) throw new IllegalArgumentException("RetrievalQuery.question must not be null");
            if (scope == null) throw new IllegalArgumentException("RetrievalQuery.scope must not be null");
            folderId = folderId == null ? Optional.empty() : folderId;
            channelId = channelId == null ? Optional.empty() : channelId;
            mentionedFileIds = mentionedFileIds == null ? List.of() : List.copyOf(mentionedFileIds);
        }
    }

    record RetrievalResult(
            List<TextHit> textHits,
            List<SpreadsheetHit> spreadsheetHits,
            boolean allBelowThreshold,
            List<String> warnings
    ) {
        public RetrievalResult {
            textHits = textHits == null ? List.of() : List.copyOf(textHits);
            spreadsheetHits = spreadsheetHits == null ? List.of() : List.copyOf(spreadsheetHits);
            warnings = warnings == null ? List.of() : List.copyOf(warnings);
        }
    }

    record TextHit(Chunk chunk, double similarity) {}
    record SpreadsheetHit(SpreadsheetSheet sheet, double similarity) {}
}
