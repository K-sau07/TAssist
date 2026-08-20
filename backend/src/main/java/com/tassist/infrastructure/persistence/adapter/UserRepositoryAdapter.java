package com.tassist.infrastructure.persistence.adapter;

import com.tassist.domain.model.User;
import com.tassist.domain.port.out.UserRepository;
import com.tassist.domain.vo.UserId;
import com.tassist.infrastructure.persistence.mapper.UserMapper;
import com.tassist.infrastructure.persistence.repo.UserJpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public class UserRepositoryAdapter implements UserRepository {
    private final UserJpaRepository jpa;
    public UserRepositoryAdapter(UserJpaRepository jpa) { this.jpa = jpa; }

    @Override public User save(User user) { return UserMapper.toDomain(jpa.save(UserMapper.toEntity(user))); }
    @Override public Optional<User> findById(UserId id) { return jpa.findById(id.value()).map(UserMapper::toDomain); }
    @Override public Optional<User> findByEmail(String email) { return jpa.findByEmail(email).map(UserMapper::toDomain); }
    @Override public Optional<User> findByGoogleSubject(String gs) { return jpa.findByGoogleSubject(gs).map(UserMapper::toDomain); }
    @Override public boolean existsByEmail(String email) { return jpa.existsByEmail(email); }
}
