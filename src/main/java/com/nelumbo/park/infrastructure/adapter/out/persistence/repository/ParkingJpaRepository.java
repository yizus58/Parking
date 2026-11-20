package com.nelumbo.park.infrastructure.adapter.out.persistence.repository;

import com.nelumbo.park.infrastructure.adapter.out.persistence.entity.ParkingEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ParkingJpaRepository extends JpaRepository<ParkingEntity, String> {

    List<ParkingEntity> findByOwnerId(String ownerId);

    /** SELECT ... FOR UPDATE on the parking row. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM ParkingEntity p WHERE p.id = :id")
    Optional<ParkingEntity> findByIdForUpdate(@Param("id") String id);
}
