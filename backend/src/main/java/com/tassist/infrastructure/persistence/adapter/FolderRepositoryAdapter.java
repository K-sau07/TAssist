package com.tassist.infrastructure.persistence.adapter;

import com.tassist.domain.model.Folder;
import com.tassist.domain.port.out.FolderRepository;
import com.tassist.domain.vo.FolderId;
import com.tassist.domain.vo.UserId;
import com.tassist.infrastructure.persistence.mapper.FolderMapper;
import com.tassist.infrastructure.persistence.repo.FolderJpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public class FolderRepositoryAdapter implements FolderRepository {
    private final FolderJpaRepository jpa;
    public FolderRepositoryAdapter(FolderJpaRepository jpa) { this.jpa = jpa; }

    @Override public Folder save(Folder folder) { return FolderMapper.toDomain(jpa.save(FolderMapper.toEntity(folder))); }
    @Override public Optional<Folder> findById(FolderId id) { return jpa.findById(id.value()).map(FolderMapper::toDomain); }
    @Override public List<Folder> findByOwner(UserId ownerId) { return jpa.findByOwnerId(ownerId.value()).stream().map(FolderMapper::toDomain).toList(); }
    @Override public boolean existsByOwnerAndName(UserId ownerId, String name) { return jpa.existsByOwnerIdAndName(ownerId.value(), name); }
    @Override public void delete(FolderId id) { jpa.deleteById(id.value()); }
}
