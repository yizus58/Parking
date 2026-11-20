package com.nelumbo.park.infrastructure.adapter.out.persistence.repository;

import com.nelumbo.park.infrastructure.adapter.out.persistence.entity.LoginLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LoginLogJpaRepository extends JpaRepository<LoginLogEntity, UUID> {
}
