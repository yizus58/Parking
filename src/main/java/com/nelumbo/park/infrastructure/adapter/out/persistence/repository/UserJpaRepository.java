package com.nelumbo.park.infrastructure.adapter.out.persistence.repository;

import com.nelumbo.park.infrastructure.adapter.out.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserJpaRepository extends JpaRepository<UserEntity, String> {

    Optional<UserEntity> findFirstByEmail(String email);

    Optional<UserEntity> findFirstByUsername(String username);
}
