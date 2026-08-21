package com.tassist.application.folder;

import com.tassist.domain.error.ConflictError;
import com.tassist.domain.error.Forbidden;
import com.tassist.domain.error.NotFoundError;
import com.tassist.domain.error.ValidationError;
import com.tassist.domain.model.File;
import com.tassist.domain.model.Folder;
import com.tassist.domain.model.FolderFile;
import com.tassist.domain.port.out.FileRepository;
import com.tassist.domain.port.out.FolderFileRepository;
import com.tassist.domain.port.out.FolderRepository;
import com.tassist.domain.vo.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

/** Unit tests for §12.3 folder service using fakes (no Spring/DB). */
class FolderServiceTest {

    static class FakeFolders implements FolderRepository {
        final Map<UUID, Folder> byId = new LinkedHashMap<>();
        public Folder save(Folder f) { byId.put(f.id().value(), f); return f; }
        public Optional<Folder> findById(FolderId id) { return Optional.ofNullable(byId.get(id.value())); }
        public List<Folder> findByOwner(UserId o) {
            return byId.values().stream().filter(f -> f.ownerId().equals(o)).toList(); }
        public boolean existsByOwnerAndName(UserId o, String n) {
            return byId.values().stream().anyMatch(f -> f.ownerId().equals(o) && f.name().equals(n)); }
        public void delete(FolderId id) { byId.remove(id.value()); }
    }
    static class FakeFolderFiles implements FolderFileRepository {
        final List<FolderFile> links = new ArrayList<>();
        public FolderFile add(FolderFile ff) { links.add(ff); return ff; }
        public void remove(FolderId fo, FileId fi) {
            links.removeIf(l -> l.folderId().equals(fo) && l.fileId().equals(fi)); }
        public List<FileId> findFileIdsByFolder(FolderId fo) {
            return links.stream().filter(l -> l.folderId().equals(fo)).map(FolderFile::fileId).toList(); }
        public List<FolderId> findFolderIdsByFile(FileId fi) {
            return links.stream().filter(l -> l.fileId().equals(fi)).map(FolderFile::folderId).toList(); }
    }
    static class FakeFiles implements FileRepository {
        final Map<UUID, File> byId = new HashMap<>();
        public File save(File f) { byId.put(f.id().value(), f); return f; }
        public Optional<File> findById(FileId id) { return Optional.ofNullable(byId.get(id.value())); }
        public List<File> findByOwner(UserId o) { return List.of(); }
        public Optional<File> findByOwnerAndContentHash(UserId o, String h) { return Optional.empty(); }
        public void delete(FileId id) { byId.remove(id.value()); }
    }

    private final FakeFolders folders = new FakeFolders();
    private final FakeFolderFiles folderFiles = new FakeFolderFiles();
    private final FakeFiles files = new FakeFiles();
    private final FolderService svc = new FolderService(folders, folderFiles, files);

    private final UserId owner = UserId.newId();
    private final UserId other = UserId.newId();

    private FileId seedFile(UserId o) {
        FileId id = FileId.newId();
        files.save(new File(id, o, "f.pdf", FileType.PDF, 10, "k", "h" + id.value(),
            FileStatus.READY, Optional.empty(), Instant.now(), Instant.now()));
        return id;
    }

    @Test void create_then_list() {
        Folder f = svc.create(owner, "Lectures");
        assertThat(f.name()).isEqualTo("Lectures");
        assertThat(svc.list(owner)).hasSize(1);
    }

    @Test void create_trims_name() {
        assertThat(svc.create(owner, "  Notes  ").name()).isEqualTo("Notes");
    }

    @Test void duplicate_name_conflicts() {
        svc.create(owner, "Dup");
        assertThatThrownBy(() -> svc.create(owner, "Dup")).isInstanceOf(ConflictError.class);
    }

    @Test void blank_name_rejected() {
        assertThatThrownBy(() -> svc.create(owner, "   ")).isInstanceOf(ValidationError.class);
    }

    @Test void rename_changes_name() {
        Folder f = svc.create(owner, "Old");
        Folder r = svc.rename(owner, f.id(), "New");
        assertThat(r.name()).isEqualTo("New");
    }

    @Test void rename_to_existing_conflicts() {
        svc.create(owner, "A");
        Folder b = svc.create(owner, "B");
        assertThatThrownBy(() -> svc.rename(owner, b.id(), "A")).isInstanceOf(ConflictError.class);
    }

    @Test void rename_others_folder_forbidden() {
        Folder f = svc.create(owner, "Mine");
        assertThatThrownBy(() -> svc.rename(other, f.id(), "X")).isInstanceOf(Forbidden.class);
    }

    @Test void add_and_list_files() {
        Folder f = svc.create(owner, "F");
        FileId fid = seedFile(owner);
        svc.addFile(owner, f.id(), fid);
        assertThat(svc.listFiles(owner, f.id())).extracting(File::id).containsExactly(fid);
    }

    @Test void add_others_file_forbidden() {
        Folder f = svc.create(owner, "F");
        FileId fid = seedFile(other);
        assertThatThrownBy(() -> svc.addFile(owner, f.id(), fid)).isInstanceOf(Forbidden.class);
    }

    @Test void add_missing_file_not_found() {
        Folder f = svc.create(owner, "F");
        assertThatThrownBy(() -> svc.addFile(owner, f.id(), FileId.newId())).isInstanceOf(NotFoundError.class);
    }

    @Test void remove_file() {
        Folder f = svc.create(owner, "F");
        FileId fid = seedFile(owner);
        svc.addFile(owner, f.id(), fid);
        svc.removeFile(owner, f.id(), fid);
        assertThat(svc.listFiles(owner, f.id())).isEmpty();
    }

    @Test void delete_folder_keeps_file() {
        Folder f = svc.create(owner, "F");
        FileId fid = seedFile(owner);
        svc.addFile(owner, f.id(), fid);
        svc.delete(owner, f.id());
        assertThat(folders.findById(f.id())).isEmpty();
        assertThat(files.findById(fid)).isPresent();  // file retained (§8)
    }

    @Test void operate_on_missing_folder_not_found() {
        assertThatThrownBy(() -> svc.delete(owner, FolderId.newId())).isInstanceOf(NotFoundError.class);
    }
}
