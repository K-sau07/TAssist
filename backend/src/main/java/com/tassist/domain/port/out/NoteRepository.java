package com.tassist.domain.port.out;

import com.tassist.domain.model.Note;
import com.tassist.domain.vo.UserId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persistence port for {@link Note} (spec §7). */
public interface NoteRepository {
    Note save(Note note);
    Optional<Note> findById(UUID id);
    List<Note> findByOwner(UserId ownerId);
    void delete(UUID id);
}
