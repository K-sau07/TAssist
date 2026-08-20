package com.tassist.infrastructure.persistence.repo;

import com.tassist.infrastructure.persistence.entity.NoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface NoteJpaRepository extends JpaRepository<NoteEntity, UUID> {
    List<NoteEntity> findByOwnerId(UUID ownerId);
}
