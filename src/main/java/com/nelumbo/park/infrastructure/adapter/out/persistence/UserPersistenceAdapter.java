package com.nelumbo.park.infrastructure.adapter.out.persistence;

import com.nelumbo.park.application.port.out.UserRepositoryPort;
import com.nelumbo.park.domain.model.User;
import com.nelumbo.park.infrastructure.adapter.out.persistence.entity.UserEntity;
import com.nelumbo.park.infrastructure.adapter.out.persistence.mapper.PersistenceMapper;
import com.nelumbo.park.infrastructure.adapter.out.persistence.repository.UserJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class UserPersistenceAdapter implements UserRepositoryPort {

    private final UserJpaRepository repository;

    public UserPersistenceAdapter(UserJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<User> findAll() {
        return repository.findAll().stream().map(PersistenceMapper::toDomain).toList();
    }

    @Override
    public Optional<User> findById(String id) {
        return repository.findById(id).map(PersistenceMapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return repository.findFirstByEmail(email).map(PersistenceMapper::toDomain);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return repository.findFirstByUsername(username).map(PersistenceMapper::toDomain);
    }

    @Override
    public User save(User user) {
        UserEntity entity = user.getId() != null
                ? repository.findById(user.getId()).orElseGet(UserEntity::new)
                : new UserEntity();
        PersistenceMapper.copy(user, entity);
        return PersistenceMapper.toDomain(repository.save(entity));
    }
}
