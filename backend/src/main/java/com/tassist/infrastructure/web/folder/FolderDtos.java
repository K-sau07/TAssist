package com.tassist.infrastructure.web.folder;

import com.tassist.domain.model.Folder;
import java.util.List;

/** §12.3 folder request/response shapes. */
public final class FolderDtos {
    private FolderDtos() {}

    public record CreateFolderRequest(String name) {}
    public record RenameFolderRequest(String name) {}
    public record AddFilesRequest(List<String> fileIds) {}

    public record FolderView(String id, String name, String createdAt) {
        public static FolderView of(Folder f) {
            return new FolderView(f.id().value().toString(), f.name(), f.createdAt().toString());
        }
    }
}
