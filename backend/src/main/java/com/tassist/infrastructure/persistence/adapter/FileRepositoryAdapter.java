package com.tassist.infrastructure.persistence.adapter;

import com.tassist.domain.model.File;
import com.tassist.domain.port.out.FileRepository;
import com.tassist.domain.vo.FileId;
import com.tassist.domain.vo.UserId;
import com.tassist.infrastructure.persistence.mapper.FileMapper;
import com.tassist.infrastructure.persistence.repo.FileJpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public class FileRepositoryAdapter implements FileRepository {
    private final FileJpaRepository jpa;
    public FileRepositoryAdapter(FileJpaRepository jpa) { this.jpa = jpa; }

    @Override public File save(File file) { return FileMapper.toDomain(jpa.save(FileMapper.toEntity(file))); }
    @Override public Optional<File> findById(FileId id) { return jpa.findById(id.value()).map(FileMapper::toDomain); }
    @Override public List<File> findByOwner(UserId ownerId) { return jpa.findByOwnerId(ownerId.value()).stream().map(FileMapper::toDomain).toList(); }
    @Override public Optional<File> findByOwnerAndContentHash(UserId ownerId, String hash) { return jpa.findByOwnerIdAndContentHash(ownerId.value(), hash).map(FileMapper::toDomain); }
    @Override public void delete(FileId id) { jpa.deleteById(id.value()); }
}
