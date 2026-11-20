package com.nelumbo.park.infrastructure.adapter.out.persistence;

import com.nelumbo.park.application.port.out.LoginLogRepositoryPort;
import com.nelumbo.park.domain.model.LoginLog;
import com.nelumbo.park.infrastructure.adapter.out.persistence.mapper.PersistenceMapper;
import com.nelumbo.park.infrastructure.adapter.out.persistence.repository.LoginLogJpaRepository;
import org.springframework.stereotype.Component;

@Component
public class LoginLogPersistenceAdapter implements LoginLogRepositoryPort {

    private final LoginLogJpaRepository repository;

    public LoginLogPersistenceAdapter(LoginLogJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(LoginLog loginLog) {
        repository.save(PersistenceMapper.toEntity(loginLog));
    }
}
