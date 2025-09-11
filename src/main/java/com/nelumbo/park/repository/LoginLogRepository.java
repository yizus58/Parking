package com.nelumbo.park.repository;

import com.nelumbo.park.entity.LoginLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface LoginLogRepository extends JpaRepository<LoginLog, UUID> {
}
