package com.tassist.infrastructure.persistence.repo;

import com.tassist.infrastructure.persistence.entity.SpreadsheetRowEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface RowJpaRepository extends JpaRepository<SpreadsheetRowEntity, UUID> {
    long countBySheetId(UUID sheetId);
}
