package com.tassist.infrastructure.persistence.adapter;

import com.tassist.domain.model.FolderFile;
import com.tassist.domain.port.out.FolderFileRepository;
import com.tassist.domain.vo.FileId;
import com.tassist.domain.vo.FolderId;
import com.tassist.infrastructure.persistence.entity.FolderFileId;
import com.tassist.infrastructure.persistence.mapper.FolderFileMapper;
import com.tassist.infrastructure.persistence.repo.FolderFileJpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public class FolderFileRepositoryAdapter implements FolderFileRepository {
    private final FolderFileJpaRepository jpa;
    public FolderFileRepositoryAdapter(FolderFileJpaRepository jpa) { this.jpa = jpa; }

    @Override public FolderFile add(FolderFile ff) { return FolderFileMapper.toDomain(jpa.save(FolderFileMapper.toEntity(ff))); }
    @Override public void remove(FolderId folderId, FileId fileId) { jpa.deleteById(new FolderFileId(folderId.value(), fileId.value())); }
    @Override public List<FileId> findFileIdsByFolder(FolderId folderId) { return jpa.findByFolderId(folderId.value()).stream().map(e -> FileId.of(e.getFileId())).toList(); }
    @Override public List<FolderId> findFolderIdsByFile(FileId fileId) { return jpa.findByFileId(fileId.value()).stream().map(e -> FolderId.of(e.getFolderId())).toList(); }
}
