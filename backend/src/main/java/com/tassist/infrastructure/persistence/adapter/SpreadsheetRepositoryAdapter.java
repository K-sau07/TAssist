package com.tassist.infrastructure.persistence.adapter;

import com.tassist.domain.model.SpreadsheetRow;
import com.tassist.domain.model.SpreadsheetSheet;
import com.tassist.domain.port.out.SpreadsheetRepository;
import com.tassist.domain.vo.FileId;
import com.tassist.infrastructure.persistence.entity.SpreadsheetSheetEntity;
import com.tassist.infrastructure.persistence.mapper.SpreadsheetRowMapper;
import com.tassist.infrastructure.persistence.mapper.SpreadsheetSheetMapper;
import com.tassist.infrastructure.persistence.repo.RowJpaRepository;
import com.tassist.infrastructure.persistence.repo.SheetJpaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Repository
public class SpreadsheetRepositoryAdapter implements SpreadsheetRepository {
    private final SheetJpaRepository sheetJpa;
    private final RowJpaRepository rowJpa;
    @PersistenceContext private EntityManager em;

    public SpreadsheetRepositoryAdapter(SheetJpaRepository sheetJpa, RowJpaRepository rowJpa) {
        this.sheetJpa = sheetJpa; this.rowJpa = rowJpa;
    }

    @Override public SpreadsheetSheet saveSheet(SpreadsheetSheet sheet) {
        return SpreadsheetSheetMapper.toDomain(sheetJpa.save(SpreadsheetSheetMapper.toEntity(sheet)));
    }

    @Override @Transactional public void saveRows(List<SpreadsheetRow> rows) {
        rowJpa.saveAll(rows.stream().map(SpreadsheetRowMapper::toEntity).toList());
    }

    @Override public List<SpreadsheetSheet> findSheetsByFile(FileId fileId) {
        return sheetJpa.findByFileId(fileId.value()).stream().map(SpreadsheetSheetMapper::toDomain).toList();
    }

    @Override public long countRowsBySheet(UUID sheetId) { return rowJpa.countBySheetId(sheetId); }

    @Override @Transactional public void deleteByFile(FileId fileId) {
        sheetJpa.deleteByFileId(fileId.value());
    }

    @Override
    public List<ScoredSheet> searchSimilarSheets(float[] queryEmbedding, List<FileId> candidateFileIds, int topK) {
        if (candidateFileIds.isEmpty()) return List.of();
        String vec = toVectorLiteral(queryEmbedding);
        List<UUID> ids = candidateFileIds.stream().map(FileId::value).toList();
        @SuppressWarnings("unchecked")
        List<SpreadsheetSheetEntity> entities = em.createNativeQuery(
                "SELECT * FROM spreadsheet_sheet WHERE file_id IN (:ids) " +
                "ORDER BY schema_summary_embedding <=> CAST(:q AS vector) LIMIT :k", SpreadsheetSheetEntity.class)
            .setParameter("q", vec).setParameter("ids", ids).setParameter("k", topK)
            .getResultList();
        @SuppressWarnings("unchecked")
        List<Tuple> scores = em.createNativeQuery(
                "SELECT id, (1 - (schema_summary_embedding <=> CAST(:q AS vector))) AS sim " +
                "FROM spreadsheet_sheet WHERE file_id IN (:ids) " +
                "ORDER BY schema_summary_embedding <=> CAST(:q AS vector) LIMIT :k", Tuple.class)
            .setParameter("q", vec).setParameter("ids", ids).setParameter("k", topK)
            .getResultList();
        return java.util.stream.IntStream.range(0, entities.size())
            .mapToObj(i -> new ScoredSheet(
                SpreadsheetSheetMapper.toDomain(entities.get(i)),
                ((Number) scores.get(i).get("sim")).doubleValue()))
            .toList();
    }

    private static String toVectorLiteral(float[] v) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < v.length; i++) { if (i > 0) sb.append(','); sb.append(v[i]); }
        return sb.append(']').toString();
    }
}
