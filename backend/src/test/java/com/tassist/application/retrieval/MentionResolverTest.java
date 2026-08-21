package com.tassist.application.retrieval;

import com.tassist.domain.model.File;
import com.tassist.domain.port.out.FileRepository;
import com.tassist.domain.vo.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

/** Unit tests for §11.4 step-2 @mention extraction + resolution. */
class MentionResolverTest {

    static class FakeFiles implements FileRepository {
        final Map<UUID, File> byId = new HashMap<>();
        final List<File> all = new ArrayList<>();
        public File save(File f) { byId.put(f.id().value(), f); all.add(f); return f; }
        public Optional<File> findById(FileId id) { return Optional.ofNullable(byId.get(id.value())); }
        public List<File> findByOwner(UserId o) { return all; }
        public Optional<File> findByOwnerAndContentHash(UserId o, String h) { return Optional.empty(); }
        public List<File> findByOwnerAndFilename(UserId o, String n) {
            return all.stream().filter(f -> f.ownerId().equals(o) && f.originalFilename().equals(n)).toList(); }
        public void delete(FileId id) {}
    }

    private final FakeFiles files = new FakeFiles();
    private final MentionResolver resolver = new MentionResolver(files);
    private final UserId user = UserId.newId();

    private FileId seed(String name) {
        FileId id = FileId.newId();
        files.save(new File(id, user, name, FileType.PDF, 1, "k", "h" + id.value(),
            FileStatus.READY, Optional.empty(), Instant.now(), Instant.now()));
        return id;
    }

    @Test void extract_simple_mentions() {
        assertThat(resolver.extractNames("compare @a.pdf and @b.pdf please"))
            .containsExactly("a.pdf", "b.pdf");
    }

    @Test void extract_quoted_name_with_spaces() {
        assertThat(resolver.extractNames("see @\"my notes.md\" here"))
            .containsExactly("my notes.md");
    }

    @Test void extract_dedupes() {
        assertThat(resolver.extractNames("@x.pdf @x.pdf")).containsExactly("x.pdf");
    }

    @Test void trailing_comma_not_part_of_name() {
        assertThat(resolver.extractNames("According to @s10.txt, how long?"))
            .containsExactly("s10.txt");
    }

    @Test void trailing_period_not_part_of_name() {
        assertThat(resolver.extractNames("see @report.pdf.")).containsExactly("report.pdf");
    }

    @Test void no_mentions_empty() {
        assertThat(resolver.extractNames("just a plain question")).isEmpty();
    }

    @Test void resolve_known_mention_to_id() {
        FileId id = seed("lecture.pdf");
        var r = resolver.resolve(user, "explain @lecture.pdf");
        assertThat(r.fileIds()).containsExactly(id);
        assertThat(r.warnings()).isEmpty();
    }

    @Test void resolve_unknown_mention_warns() {
        var r = resolver.resolve(user, "explain @ghost.pdf");
        assertThat(r.fileIds()).isEmpty();
        assertThat(r.warnings()).containsExactly("File 'ghost.pdf' not found; ignoring.");
    }

    @Test void resolve_mixed_known_and_unknown() {
        FileId id = seed("real.pdf");
        var r = resolver.resolve(user, "@real.pdf vs @fake.pdf");
        assertThat(r.fileIds()).containsExactly(id);
        assertThat(r.warnings()).hasSize(1);
    }

    @Test void duplicate_filename_resolves_to_all_matches() {
        FileId id1 = seed("dup.pdf");
        FileId id2 = seed("dup.pdf"); // same name, allowed (unique is on content_hash)
        var r = resolver.resolve(user, "@dup.pdf");
        assertThat(r.fileIds()).containsExactlyInAnyOrder(id1, id2);
    }
}
