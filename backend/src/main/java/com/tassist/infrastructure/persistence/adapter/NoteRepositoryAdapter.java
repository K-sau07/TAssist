package com.tassist.infrastructure.persistence.adapter;

import com.tassist.domain.model.Note;
import com.tassist.domain.port.out.NoteRepository;
import com.tassist.domain.vo.UserId;
import com.tassist.infrastructure.persistence.mapper.NoteMapper;
import com.tassist.infrastructure.persistence.repo.NoteJpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class NoteRepositoryAdapter implements NoteRepository {
    private final NoteJpaRepository jpa;
    public NoteRepositoryAdapter(NoteJpaRepository jpa) { this.jpa = jpa; }

    @Override public Note save(Note n) { return NoteMapper.toDomain(jpa.save(NoteMapper.toEntity(n))); }
    @Override public Optional<Note> findById(UUID id) { return jpa.findById(id).map(NoteMapper::toDomain); }
    @Override public List<Note> findByOwner(UserId ownerId) { return jpa.findByOwnerId(ownerId.value()).stream().map(NoteMapper::toDomain).toList(); }
    @Override public void delete(UUID id) { jpa.deleteById(id); }
}
