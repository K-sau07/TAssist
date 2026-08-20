package com.tassist.infrastructure.persistence.repo;

import com.tassist.infrastructure.persistence.entity.SpreadsheetSheetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

public interface SheetJpaRepository extends JpaRepository<SpreadsheetSheetEntity, UUID> {
    List<SpreadsheetSheetEntity> findByFileId(UUID fileId);
    @Modifying @Query("delete from SpreadsheetSheetEntity s where s.fileId = :fileId")
    void deleteByFileId(@Param("fileId") UUID fileId);
}
